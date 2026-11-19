package com.smartlocker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class TeacherDtos {
    public record CreateStudentRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-]{1,19}$",
                    message = "Use 2 to 20 letters, numbers, or hyphens for the roll number")
            String rollNumber,
            @Size(min = 8, max = 72) String specialPassword) {
    }

    public record StudentSummary(Long studentId, String name, String rollNumber, String standard) {
    }

    public record CreatedStudent(StudentSummary student, String specialPassword) {
    }

    public record TimetableEntryRequest(
            @NotBlank @Size(max = 40) String period,
            @NotBlank @Size(max = 100) String subject,
            boolean homeworkRequired) {}

    public record TimetableSaveRequest(
            @NotNull DayOfWeek dayOfWeek,
            @Size(max = 12) List<TimetableEntryRequest> entries) {}

    public record TimetableEntryResponse(Long id, String period, String subject,
                                         boolean homeworkRequired) {}

    public record ClassNoteRequest(
            @NotNull LocalDate date,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 4000) String content,
            @NotBlank @Size(max = 40) String category) {}

    public record ClassNoteResponse(Long id, LocalDate date, String title,
                                    String content, String category) {}

}
