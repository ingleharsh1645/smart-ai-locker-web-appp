package com.smartlocker.controller;

import com.smartlocker.dto.TeacherDtos.ClassNoteRequest;
import com.smartlocker.dto.TeacherDtos.CreateStudentRequest;
import com.smartlocker.dto.TeacherDtos.CreatedStudent;
import com.smartlocker.dto.TeacherDtos.StudentSummary;
import com.smartlocker.dto.TeacherDtos.TimetableSaveRequest;
import com.smartlocker.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/students")
    public List<StudentSummary> students() {
        return teacherService.students();
    }

    @PostMapping("/students")
    public CreatedStudent createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return teacherService.createStudent(request);
    }

    @GetMapping("/timetable/{day}")
    public java.util.Map<String, Object> timetable(@PathVariable DayOfWeek day) {
        return teacherService.timetable(day);
    }

    @PutMapping("/timetable")
    public java.util.Map<String, Object> saveTimetable(@Valid @RequestBody TimetableSaveRequest request) {
        return teacherService.saveTimetable(request);
    }

    @GetMapping("/notes")
    public List<com.smartlocker.dto.TeacherDtos.ClassNoteResponse> notes() {
        return teacherService.notes();
    }

    @PostMapping("/notes")
    public com.smartlocker.dto.TeacherDtos.ClassNoteResponse saveNote(@Valid @RequestBody ClassNoteRequest request) {
        return teacherService.saveNote(request);
    }
}
