package com.smartlocker.service;

import com.smartlocker.dto.LockerDtos.ChecklistItem;
import com.smartlocker.entity.LockerActivity;
import com.smartlocker.entity.LockerItem;
import com.smartlocker.entity.Student;
import com.smartlocker.entity.TimetableEntry;
import com.smartlocker.repository.LockerActivityRepository;
import com.smartlocker.repository.LockerItemRepository;
import com.smartlocker.repository.StudentRepository;
import com.smartlocker.repository.TimetableEntryRepository;
import com.smartlocker.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LockerServiceTest {
    private Student student;
    private LockerItemRepository items;
    private TimetableEntryRepository timetable;
    private LockerActivityRepository activities;
    private LockerService service;

    @BeforeEach
    void setUp() {
        student = Student.builder().id(10L).rollNumber("C7-101").name("Aarav").standard("7").build();
        items = mock(LockerItemRepository.class);
        timetable = mock(TimetableEntryRepository.class);
        activities = mock(LockerActivityRepository.class);
        service = new LockerService(mock(StudentRepository.class), items, timetable, activities, mock(UserAccountRepository.class));
    }

    @Test
    void checklistUsesTodaysHomeworkSubjects() {
        DayOfWeek today = LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek();
        when(timetable.findByDayOfWeek(today)).thenReturn(List.of(
                TimetableEntry.builder().dayOfWeek(today).period("Period 1").subject("Science").homeworkRequired(true).build(),
                TimetableEntry.builder().dayOfWeek(today).period("Period 2").subject("Hindi").homeworkRequired(false).build()));
        when(items.findByStudentId(student.getId())).thenReturn(List.of(
                LockerItem.builder().id(1L).student(student).name("Science Book").category("Book").subject("science").build(),
                LockerItem.builder().id(2L).student(student).name("Hindi Book").category("Book").subject("Hindi").build()));

        var checklist = service.checklist(student);

        ChecklistItem science = checklist.checklist().stream().filter(i -> i.itemId().equals(1L)).findFirst().orElseThrow();
        ChecklistItem hindi = checklist.checklist().stream().filter(i -> i.itemId().equals(2L)).findFirst().orElseThrow();
        assertTrue(science.requiredToday());
        assertEquals("TAKE_HOME", science.action());
        assertFalse(hindi.requiredToday());
        assertEquals("LEAVE_IN_LOCKER", hindi.action());
    }

    @Test
    void monitorUsesMostRecentActionWhenCalculatingMissedHomeworkItems() {
        DayOfWeek today = LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek();
        LockerItem science = LockerItem.builder().id(1L).student(student).name("Science Book").category("Book").subject("Science").build();
        TimetableEntry scienceHomework = TimetableEntry.builder().dayOfWeek(today).period("Period 1").subject("Science").homeworkRequired(true).build();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        LockerActivity keptMostRecently = LockerActivity.builder().id(2L).student(student).lockerItem(science)
                .action(LockerActivity.ActivityAction.KEPT_IN_LOCKER).timestamp(now).build();
        LockerActivity takenEarlier = LockerActivity.builder().id(1L).student(student).lockerItem(science)
                .action(LockerActivity.ActivityAction.TAKEN_FROM_LOCKER).timestamp(now.minusMinutes(1)).build();
        when(timetable.findByDayOfWeek(today)).thenReturn(List.of(scienceHomework));
        when(activities.findByStudentIdAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampDesc(
                eq(student.getId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(keptMostRecently, takenEarlier));
        when(items.findByStudentId(student.getId())).thenReturn(List.of(science));

        var monitor = service.monitor(student);

        assertEquals(List.of("Science Book"), monitor.missedRequiredItems());
        assertFalse(monitor.homeworkChecklistComplete());
        assertEquals(2, monitor.todayLogs().size());
    }

    @Test
    void monitorRetrievesOnlyTheRequestedDayAndRejectsFutureDates() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate yesterday = LocalDate.now(zone).minusDays(1);
        LocalDateTime start = yesterday.atStartOfDay();
        LockerItem science = LockerItem.builder().id(1L).student(student).name("Science Book")
                .category("Book").subject("Science").build();
        LockerActivity event = LockerActivity.builder().id(3L).student(student).lockerItem(science)
                .action(LockerActivity.ActivityAction.TAKEN_FROM_LOCKER)
                .timestamp(start.plusHours(8)).build();
        when(activities.findByStudentIdAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampDesc(
                student.getId(), start, yesterday.plusDays(1).atStartOfDay())).thenReturn(List.of(event));
        when(items.findByStudentId(student.getId())).thenReturn(List.of(science));

        var report = service.monitor(student, yesterday);

        assertEquals(yesterday, report.reportDate());
        assertEquals(1, report.todayLogs().size());
        verify(timetable).findByDayOfWeek(yesterday.getDayOfWeek());
        assertThrows(com.smartlocker.exception.ApiException.class,
                () -> service.monitor(student, LocalDate.now(zone).plusDays(1)));
    }
}
