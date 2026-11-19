package com.smartlocker.service;

import com.smartlocker.dto.TeacherDtos.*;
import com.smartlocker.entity.ClassNote;
import com.smartlocker.entity.Student;
import com.smartlocker.entity.TimetableEntry;
import com.smartlocker.exception.ApiException;
import com.smartlocker.repository.ClassNoteRepository;
import com.smartlocker.repository.StudentRepository;
import com.smartlocker.repository.TimetableEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class TeacherService {
    private final StudentRepository students;
    private final TimetableEntryRepository timetable;
    private final ClassNoteRepository notes;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public TeacherService(StudentRepository students, TimetableEntryRepository timetable,
                         ClassNoteRepository notes, PasswordEncoder passwordEncoder) {
        this.students = students;
        this.timetable = timetable;
        this.notes = notes;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<StudentSummary> students() {
        return students.findAll().stream()
                .map(student -> new StudentSummary(student.getId(), student.getName(),
                        student.getRollNumber(), student.getStandard()))
                .toList();
    }

    @Transactional
    public CreatedStudent createStudent(CreateStudentRequest request) {
        String rollNumber = request.rollNumber().trim().toUpperCase(Locale.ROOT);
        if (students.findByRollNumber(rollNumber).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "That student roll number already exists");
        }

        String specialPassword = request.specialPassword();
        if (specialPassword == null || specialPassword.isBlank()) {
            specialPassword = generateSpecialPassword();
        } else if (specialPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The special password must be no more than 72 UTF-8 bytes");
        }
        Student student = students.save(Student.builder()
                .name(request.name().trim())
                .rollNumber(rollNumber)
                .standard("7")
                .specialPassword(passwordEncoder.encode(specialPassword))
                .build());
        return new CreatedStudent(
                new StudentSummary(student.getId(), student.getName(), student.getRollNumber(), student.getStandard()),
                specialPassword);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> timetable(java.time.DayOfWeek day) {
        List<TimetableEntryResponse> entries = timetable.findByDayOfWeekOrderByIdAsc(day).stream()
                .map(entry -> new TimetableEntryResponse(entry.getId(), entry.getPeriod(),
                        entry.getSubject(), entry.isHomeworkRequired()))
                .toList();
        return java.util.Map.of("dayOfWeek", day, "entries", entries);
    }

    @Transactional
    public java.util.Map<String, Object> saveTimetable(TimetableSaveRequest request) {
        timetable.deleteByDayOfWeek(request.dayOfWeek());
        for (TimetableEntryRequest entryRequest : request.entries()) {
            timetable.save(TimetableEntry.builder()
                    .dayOfWeek(request.dayOfWeek())
                    .period(entryRequest.period().trim())
                    .subject(entryRequest.subject().trim())
                    .homeworkRequired(entryRequest.homeworkRequired())
                    .build());
        }
        return timetable(request.dayOfWeek());
    }

    @Transactional
    public ClassNoteResponse saveNote(ClassNoteRequest request) {
        ClassNote note = ClassNote.builder()
                .date(request.date())
                .title(request.title().trim())
                .content(request.content().trim())
                .category(request.category().trim())
                .build();
        note = notes.save(note);
        return new ClassNoteResponse(note.getId(), note.getDate(), note.getTitle(),
                note.getContent(), note.getCategory());
    }

    @Transactional(readOnly = true)
    public List<ClassNoteResponse> notes() {
        return notes.findByDateGreaterThanEqualOrderByDateDesc(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")))
                .stream().map(note -> new ClassNoteResponse(note.getId(), note.getDate(),
                        note.getTitle(), note.getContent(), note.getCategory())).toList();
    }

    private String generateSpecialPassword() {
        byte[] randomBytes = new byte[12];
        secureRandom.nextBytes(randomBytes);
        return "C7-" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
