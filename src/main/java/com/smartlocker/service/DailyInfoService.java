package com.smartlocker.service;

import com.smartlocker.repository.ClassNoteRepository;
import com.smartlocker.repository.TimetableEntryRepository;
import com.smartlocker.repository.WeeklyParipathScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Service
public class DailyInfoService {
    private final WeeklyParipathScheduleRepository schedule;
    private final TimetableEntryRepository timetable;
    private final ClassNoteRepository notes;

    public DailyInfoService(WeeklyParipathScheduleRepository schedule, TimetableEntryRepository timetable,
                            ClassNoteRepository notes) {
        this.schedule = schedule;
        this.timetable = timetable;
        this.notes = notes;
    }

    public Map<String, Object> today() {
        DayOfWeek day = LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek();
        var paripath = schedule.findByDayOfWeek(day).orElse(null);
        var entries = timetable.findByDayOfWeekOrderByIdAsc(day).stream()
                .map(item -> Map.of("period", item.getPeriod(), "subject", item.getSubject(),
                        "homeworkRequired", item.isHomeworkRequired()))
                .toList();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        var classNotes = notes.findByDateGreaterThanEqualOrderByDateDesc(today).stream()
                .filter(note -> note.getDate().equals(today))
                .map(note -> Map.of("id", note.getId(), "title", note.getTitle(),
                        "content", note.getContent(), "category", note.getCategory(), "date", note.getDate().toString()))
                .toList();
        if (paripath != null && paripath.isParipathDay()) {
            return Map.of("day", day, "paripathDay", true,
                    "importantNews", String.valueOf(paripath.getImportantNews()),
                    "suvichar", String.valueOf(paripath.getSuvichar()),
                    "assemblyInfo", String.valueOf(paripath.getAssemblyInfo()),
                    "timetable", entries, "notes", classNotes);
        }
        return Map.of("day", day, "paripathDay", false,
                "importantNews", "General Class 7 classroom update",
                "suvichar", "Stay curious and complete today's learning goals.",
                "assemblyInfo", "No Class 7 Paripath assembly today.", "timetable", entries, "notes", classNotes);
    }
}
