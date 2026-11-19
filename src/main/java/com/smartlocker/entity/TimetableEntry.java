package com.smartlocker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.DayOfWeek;

@Entity @Table(name="timetable_entries", uniqueConstraints=@UniqueConstraint(columnNames={"day_of_week","period"})) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimetableEntry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private DayOfWeek dayOfWeek;
    @Column(nullable=false)
    private String period;
    @Column(nullable=false)
    private String subject;
    private boolean homeworkRequired;
}
