package com.smartlocker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlocker.dto.ParipathDtos.GeneratedContent;
import com.smartlocker.dto.ParipathDtos.ParipathResponse;
import com.smartlocker.dto.ParipathDtos.ScheduleRequest;
import com.smartlocker.entity.ParipathSchedule;
import com.smartlocker.exception.ApiException;
import com.smartlocker.repository.ParipathScheduleRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ParipathService {
    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Kolkata");

    private final ParipathScheduleRepository schedules;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;

    public ParipathService(
            ParipathScheduleRepository schedules,
            ObjectMapper objectMapper,
            ObjectProvider<ChatClient.Builder> chatClientBuilders) {
        this.schedules = schedules;
        this.objectMapper = objectMapper;
        ChatClient.Builder builder = chatClientBuilders.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
    }

    @Transactional
    public ParipathResponse today() {
        LocalDate today = LocalDate.now(SCHOOL_ZONE);
        ParipathSchedule schedule = schedules.findByDate(today).orElse(null);
        if (schedule == null || !schedule.isClass7Turn()) {
            return new ParipathResponse(false,
                    "Today is not Class 7's Paripath turn.", today, today.getDayOfWeek(),
                    null, null, List.of(), null, null, null, false, false);
        }

        if (isIncomplete(schedule)) {
            GeneratedContent generated = generateContent(today, schedule.getDayOfWeek());
            mergeGeneratedContent(schedule, generated);
            schedule.setAiGenerated(true);
            schedules.save(schedule);
        }
        return toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<ParipathResponse> upcoming() {
        return schedules.findByDateGreaterThanEqualOrderByDateAsc(LocalDate.now(SCHOOL_ZONE))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ParipathResponse save(LocalDate date, ScheduleRequest request) {
        ParipathSchedule schedule = schedules.findByDate(date)
                .orElseGet(() -> ParipathSchedule.builder().date(date).build());
        schedule.setDate(date);
        schedule.setDayOfWeek(date.getDayOfWeek());
        schedule.setClass7Turn(request.isClass7Turn());
        schedule.setSuvichar(clean(request.suvichar()));
        schedule.setSuvicharMeaning(clean(request.suvicharMeaning()));
        schedule.setTodaysNewsJson(writeNews(request.todaysNews()));
        schedule.setSpecialDayInfo(clean(request.specialDayInfo()));
        schedule.setAssemblyRoleAssignment(clean(request.assemblyRoleAssignment()));
        schedule.setGkQuestion(clean(request.gkQuestion()));
        schedule.setAiGenerated(false);
        return toResponse(schedules.save(schedule));
    }

    private boolean isIncomplete(ParipathSchedule schedule) {
        return blank(schedule.getSuvichar())
                || blank(schedule.getSuvicharMeaning())
                || readNews(schedule).size() < 3
                || blank(schedule.getSpecialDayInfo());
    }

    private GeneratedContent generateContent(LocalDate date, DayOfWeek day) {
        if (chatClient == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Paripath content is incomplete and Gemini is not configured. Set GEMINI_API_KEY or ask a teacher to enter the content.");
        }
        String response;
        try {
            response = chatClient.prompt()
                    .system("""
                            Create age-appropriate Class 7 school assembly suggestions. Return only valid JSON with exactly these fields:
                            {"suvichar":"A short inspiring quote in Hindi, Marathi, or English","suvicharMeaning":"A simple meaning in English","todaysNews":["three or four positive educational headline suggestions"],"specialDayInfo":"A brief educational date-specific fact"}
                            Keep headlines non-political, positive and safe for children. You do not have live news sources: never claim an item is verified breaking news; write evergreen science, education, environment or civic learning headline suggestions instead. Do not include markdown or extra fields.
                            """)
                    .user("Prepare suggestions for " + date + " (" + day + ") for a Class 7 school assembly.")
                    .call()
                    .content();
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Gemini could not generate Paripath content. Please retry or ask a teacher to enter it.");
        }
        try {
            GeneratedContent generated = objectMapper.readValue(response, GeneratedContent.class);
            validateGeneratedContent(generated);
            return generated;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Gemini returned invalid Paripath content. Please retry or ask a teacher to enter it.");
        }
    }

    private void validateGeneratedContent(GeneratedContent generated) {
        if (generated == null
                || blank(generated.suvichar())
                || blank(generated.suvicharMeaning())
                || blank(generated.specialDayInfo())
                || generated.todaysNews() == null
                || generated.todaysNews().size() < 3
                || generated.todaysNews().size() > 4
                || generated.todaysNews().stream().anyMatch(ParipathService::blank)) {
            throw new IllegalArgumentException("Generated Paripath content is incomplete");
        }
    }

    private void mergeGeneratedContent(ParipathSchedule schedule, GeneratedContent generated) {
        if (blank(schedule.getSuvichar())) {
            schedule.setSuvichar(generated.suvichar());
        }
        if (blank(schedule.getSuvicharMeaning())) {
            schedule.setSuvicharMeaning(generated.suvicharMeaning());
        }
        List<String> existingNews = readNews(schedule);
        if (existingNews.size() < 3) {
            java.util.LinkedHashSet<String> combinedNews = new java.util.LinkedHashSet<>(existingNews);
            combinedNews.addAll(generated.todaysNews());
            schedule.setTodaysNewsJson(writeNews(combinedNews.stream().limit(4).toList()));
        }
        if (blank(schedule.getSpecialDayInfo())) {
            schedule.setSpecialDayInfo(generated.specialDayInfo());
        }
    }

    private ParipathResponse toResponse(ParipathSchedule schedule) {
        return new ParipathResponse(schedule.isClass7Turn(),
                schedule.isClass7Turn() ? null : "Today is not Class 7's Paripath turn.",
                schedule.getDate(), schedule.getDayOfWeek(), schedule.getSuvichar(),
                schedule.getSuvicharMeaning(), readNews(schedule), schedule.getSpecialDayInfo(),
                schedule.getAssemblyRoleAssignment(), schedule.getGkQuestion(),
                schedule.isAiGenerated(), schedule.isAiGenerated());
    }

    private List<String> readNews(ParipathSchedule schedule) {
        if (blank(schedule.getTodaysNewsJson())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(schedule.getTodaysNewsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Saved Paripath news could not be read. Ask a teacher to update the schedule.");
        }
    }

    private String writeNews(List<String> news) {
        if (news == null || news.isEmpty()) {
            return null;
        }
        List<String> cleaned = news.stream().filter(item -> item != null && !item.isBlank())
                .map(String::trim).toList();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Paripath news could not be saved.");
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
