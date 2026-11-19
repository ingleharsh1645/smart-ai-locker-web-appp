package com.smartlocker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.DayOfWeek;

@Entity @Table(name="paripath_schedule", uniqueConstraints=@UniqueConstraint(columnNames="day_of_week")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WeeklyParipathSchedule {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private DayOfWeek dayOfWeek;
    @Column(nullable=false)
    private boolean paripathDay;
    @Column(length=2000)
    private String importantNews;
    @Column(length=2000)
    private String suvichar;
    @Column(length=2000)
    private String assemblyInfo;
}
