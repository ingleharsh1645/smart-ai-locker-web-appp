package com.smartlocker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlocker.entity.ParipathSchedule;
import com.smartlocker.repository.ParipathScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParipathServiceTest {
    @Test
    void fillsOnlyMissingParipathSectionsAndRetainsManualContent() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        ParipathSchedule schedule = ParipathSchedule.builder()
                .date(today)
                .dayOfWeek(today.getDayOfWeek())
                .isClass7Turn(true)
                .suvichar("Teacher's original thought")
                .todaysNewsJson("[\"Teacher-approved headline\"]")
                .build();
        ParipathScheduleRepository repository = mock(ParipathScheduleRepository.class);
        when(repository.findByDate(today)).thenReturn(Optional.of(schedule));
        when(repository.save(any(ParipathSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("""
                        {"suvichar":"AI thought","suvicharMeaning":"A simple explanation","todaysNews":["Headline one","Headline two","Headline three"],"specialDayInfo":"A science fact for today"}
                        """);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("chatClientBuilder", builder);
        ObjectProvider<ChatClient.Builder> chatClientBuilders = beanFactory.getBeanProvider(ChatClient.Builder.class);

        ParipathService service = new ParipathService(repository, new ObjectMapper(), chatClientBuilders);
        var response = service.today();

        assertTrue(response.isParipathDay());
        assertTrue(response.aiGenerated());
        assertEquals("Teacher's original thought", response.suvichar());
        assertEquals("A simple explanation", response.suvicharMeaning());
        assertEquals(List.of("Teacher-approved headline", "Headline one", "Headline two", "Headline three"),
                response.todaysNews());
        assertEquals("A science fact for today", response.specialDayInfo());
    }
}
