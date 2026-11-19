package com.smartlocker.dto;

import com.smartlocker.entity.LockerActivity.ActivityAction; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import jakarta.validation.constraints.Pattern; import jakarta.validation.constraints.Size; import java.time.*; import java.util.List;

public class LockerDtos {
    public record TimetableItem(String period,String subject,boolean homeworkRequired) {}
    public record ChecklistItem(Long itemId,String name,String category,String subject,boolean requiredToday,String action) {}
    public record SmartChecklist(List<TimetableItem> timetable,List<ChecklistItem> checklist,DayOfWeek day) {}
    public record LockerItemRequest(
            @NotBlank @Size(max=120) String name,
            @NotBlank @Pattern(regexp="(?i)BOOK|NOTEBOOK|OTHER",message="Category must be Book, Notebook, or Other") String category,
            @NotBlank @Size(max=100) String subject,
            boolean requiredToday) {}
    public record ActivityRequest(@NotNull Long itemId,@NotNull ActivityAction action) {}
    public record ActivityLog(Long id,Long itemId,String itemName,ActivityAction action,LocalDateTime timestamp) {}
    public record ParentMonitor(String studentName,String rollNumber,LocalDate reportDate,List<ActivityLog> todayLogs,List<String> missedRequiredItems,boolean homeworkChecklistComplete) {}
}
