package com.smartlocker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class ParipathDtos {
    public record ParipathResponse(
            boolean isParipathDay,
            String message,
            LocalDate date,
            DayOfWeek dayOfWeek,
            String suvichar,
            String suvicharMeaning,
            List<String> todaysNews,
            String specialDayInfo,
            String assemblyRoleAssignment,
            String gkQuestion,
            boolean aiGenerated,
            boolean teacherReviewRecommended) {
    }

    public record ScheduleRequest(
            @NotNull Boolean isClass7Turn,
            @Size(max = 1000) String suvichar,
            @Size(max = 1000) String suvicharMeaning,
            @Size(max = 4) List<@Size(max = 300) String> todaysNews,
            @Size(max = 2000) String specialDayInfo,
            @Size(max = 4000) String assemblyRoleAssignment,
            @Size(max = 2000) String gkQuestion) {
    }

    public record GeneratedContent(
            String suvichar,
            String suvicharMeaning,
            List<String> todaysNews,
            String specialDayInfo) {
    }
}
