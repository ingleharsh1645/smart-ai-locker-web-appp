package com.smartlocker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Entity
@Table(name = "class7_paripath_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParipathSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_date", nullable = false, unique = true)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "is_class7_turn", nullable = false)
    private boolean isClass7Turn;

    @Column(length = 1000)
    private String suvichar;

    @Column(length = 1000)
    private String suvicharMeaning;

    @Column(name = "todays_news_json", length = 4000)
    private String todaysNewsJson;

    @Column(length = 2000)
    private String specialDayInfo;

    @Column(length = 4000)
    private String assemblyRoleAssignment;

    @Column(length = 2000)
    private String gkQuestion;

    @Column(nullable = false)
    @Builder.Default
    private boolean aiGenerated = false;
}
