package com.smartlocker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlocker.entity.LockerActivity;
import com.smartlocker.entity.LockerItem;
import com.smartlocker.entity.Student;
import com.smartlocker.entity.TimetableEntry;
import com.smartlocker.repository.LockerActivityRepository;
import com.smartlocker.repository.LockerItemRepository;
import com.smartlocker.repository.WeeklyParipathScheduleRepository;
import com.smartlocker.repository.ParipathScheduleRepository;
import com.smartlocker.repository.StudentRepository;
import com.smartlocker.repository.TimetableEntryRepository;
import com.smartlocker.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:locker;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.jwt.secret=test-secret-that-is-long-enough-for-hmac-sha-256",
        "app.teacher.access-key=test-teacher-key-that-is-at-least-thirty-two-characters",
        "spring.ai.google.genai.api-key=test-api-key"
})
class ApplicationSmokeTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StudentRepository students;
    @Autowired private LockerItemRepository items;
    @Autowired private TimetableEntryRepository timetable;
    @Autowired private LockerActivityRepository activities;
    @Autowired private WeeklyParipathScheduleRepository schedules;
    @Autowired private ParipathScheduleRepository paripathSchedules;
    @Autowired private UserAccountRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    @org.junit.jupiter.api.BeforeEach
    void clearDatabase() {
        activities.deleteAll();
        items.deleteAll();
        timetable.deleteAll();
        schedules.deleteAll();
        paripathSchedules.deleteAll();
        users.deleteAll();
        students.deleteAll();
    }

    @Test
    void dailyInfoIsPublicAndLockerRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/sw.js"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/class7/daily-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.day").exists())
                .andExpect(jsonPath("$.paripathDay").exists());

        mockMvc.perform(get("/api/locker/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentAndParentFlowsRespectOwnershipAndLatestLockerAction() throws Exception {
        Student student = students.save(Student.builder()
                .rollNumber("C7-101")
                .name("Aarav")
                .standard("7")
                .specialPassword(passwordEncoder.encode("Special7!"))
                .build());
        LockerItem scienceBook = items.save(LockerItem.builder()
                .student(student)
                .name("Science Book")
                .category("Book")
                .subject("Science")
                .build());
        DayOfWeek today = LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek();
        timetable.save(TimetableEntry.builder()
                .dayOfWeek(today)
                .period("Period 1")
                .subject("Science")
                .homeworkRequired(true)
                .build());

        String studentToken = loginStudent();
        mockMvc.perform(get("/api/locker/today").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklist[0].requiredToday").value(true));

        var newItemResult = mockMvc.perform(post("/api/locker/items")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"English Notebook","category":"NOTEBOOK","subject":"English","requiredToday":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Notebook"))
                .andReturn();
        long newItemId = objectMapper.readTree(newItemResult.getResponse().getContentAsString()).get("itemId").asLong();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/locker/items/{itemId}", newItemId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"English Revision Notebook","category":"Notebook","subject":"English","requiredToday":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("English Revision Notebook"));
        Student otherStudent = students.save(Student.builder()
                .rollNumber("C7-102")
                .name("Mira")
                .standard("7")
                .specialPassword(passwordEncoder.encode("Another7!"))
                .build());
        LockerItem anotherStudentItem = items.save(LockerItem.builder()
                .student(otherStudent)
                .name("Private Book")
                .category("Book")
                .subject("Science")
                .build());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/locker/items/{itemId}", anotherStudentItem.getId())
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Changed Book","category":"Book","subject":"Science","requiredToday":false}
                                """))
                .andExpect(status().isForbidden());

        recordActivity(studentToken, scienceBook.getId(), "TAKEN_FROM_LOCKER");
        mockMvc.perform(get("/api/locker/activity/today").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("TAKEN_FROM_LOCKER"));

        var parentRegistration = mockMvc.perform(post("/api/auth/parent/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mobile":"9876543210","email":"parent@example.com","password":"ParentPass7!","rollNumber":"C7-101","specialPassword":"Special7!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String parentToken = objectMapper.readTree(parentRegistration.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/parent/monitor").header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayLogs.length()").value(1))
                .andExpect(jsonPath("$.homeworkChecklistComplete").value(true));
        String reportDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ISO_DATE);
        mockMvc.perform(get("/api/parent/monitor").param("date", reportDate)
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportDate").value(reportDate))
                .andExpect(jsonPath("$.todayLogs.length()").value(1));
        String futureDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1).format(DateTimeFormatter.ISO_DATE);
        mockMvc.perform(get("/api/parent/monitor").param("date", futureDate)
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/parent/monitor").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/locker/today").header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isForbidden());

        recordActivity(studentToken, scienceBook.getId(), "KEPT_IN_LOCKER");
        mockMvc.perform(get("/api/locker/activity/today").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("KEPT_IN_LOCKER"));
        mockMvc.perform(get("/api/parent/monitor").header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missedRequiredItems[0]").value("Science Book"))
                .andExpect(jsonPath("$.homeworkChecklistComplete").value(false));
    }

    @Test
    void parentRegistrationNormalizesFormattedMobileAndAllowsFormattedMobileLogin() throws Exception {
        students.save(Student.builder()
                .rollNumber("C7-101")
                .name("Aarav")
                .standard("7")
                .specialPassword(passwordEncoder.encode("Special7!"))
                .build());

        mockMvc.perform(post("/api/auth/parent/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mobile":"+91 98765 43210","email":"","password":"ParentPass7!","rollNumber":"C7-101","specialPassword":"Special7!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PARENT"));

        assertTrue(users.findByMobile("+919876543210").isPresent());
        mockMvc.perform(post("/api/auth/parent/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identity":"+91 (98765) 43210","password":"ParentPass7!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PARENT"));

        mockMvc.perform(post("/api/auth/parent/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","mobile":"","password":"ParentPass7!","rollNumber":"C7-101","specialPassword":"Special7!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email or mobile is required"));
    }

    @Test
    void teacherCanProvisionStudentAndStudentCanLogInWithGeneratedPassword() throws Exception {
        mockMvc.perform(get("/api/teacher/students"))
                .andExpect(status().isUnauthorized());
        students.save(Student.builder()
                .rollNumber("C7-101")
                .name("Aarav")
                .standard("7")
                .specialPassword(passwordEncoder.encode("Special7!"))
                .build());
        String studentToken = loginStudent();
        mockMvc.perform(get("/api/teacher/students")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        String teacherToken = loginTeacher();
        String createResponse = mockMvc.perform(post("/api/teacher/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mira Patel","rollNumber":"c7-205"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.rollNumber").value("C7-205"))
                .andExpect(jsonPath("$.student.standard").value("7"))
                .andExpect(jsonPath("$.specialPassword").isNotEmpty())
                .andExpect(jsonPath("$.student.specialPassword").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String generatedPassword = objectMapper.readTree(createResponse).get("specialPassword").asText();

        mockMvc.perform(post("/api/auth/student/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "rollNumber", "C7-205", "specialPassword", generatedPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        mockMvc.perform(post("/api/teacher/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Duplicate","rollNumber":"C7-205"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void datedParipathIsPublicOnlyOnItsConfiguredClassSevenTurn() throws Exception {
        mockMvc.perform(get("/api/class7/paripath/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isParipathDay").value(false))
                .andExpect(jsonPath("$.message").value("Today is not Class 7's Paripath turn."));

        String today = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ISO_DATE);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/teacher/paripath/{date}", today)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isClass7Turn":false}
                                """))
                .andExpect(status().isUnauthorized());
        String teacherToken = loginTeacher();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/teacher/paripath/{date}", today)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isClass7Turn": true,
                                  "suvichar": "Learning grows when we share.",
                                  "suvicharMeaning": "Sharing knowledge helps everyone.",
                                  "todaysNews": ["Science clubs open", "Students plant trees", "Library week begins"],
                                  "specialDayInfo": "A day to celebrate curiosity.",
                                  "assemblyRoleAssignment": "Suvichar: Mira"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isParipathDay").value(true));

        mockMvc.perform(get("/api/class7/paripath/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isParipathDay").value(true))
                .andExpect(jsonPath("$.suvichar").value("Learning grows when we share."))
                .andExpect(jsonPath("$.todaysNews.length()").value(3))
                .andExpect(jsonPath("$.assemblyRoleAssignment").value("Suvichar: Mira"))
                .andExpect(jsonPath("$.aiGenerated").value(false));
    }

    private String loginStudent() throws Exception {
        var result = mockMvc.perform(post("/api/auth/student/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rollNumber":"C7-101","specialPassword":"Special7!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertTrue(!token.isBlank());
        return token;
    }

    private String loginTeacher() throws Exception {
        var result = mockMvc.perform(post("/api/auth/teacher/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accessKey":"test-teacher-key-that-is-at-least-thirty-two-characters"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private void recordActivity(String token, Long itemId, String action) throws Exception {
        mockMvc.perform(post("/api/locker/activity")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("itemId", itemId, "action", action))))
                .andExpect(status().isOk());
    }
}
