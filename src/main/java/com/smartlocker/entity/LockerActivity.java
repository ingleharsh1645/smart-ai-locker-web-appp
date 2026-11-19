package com.smartlocker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="locker_activities") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LockerActivity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="student_id")
    private Student student;
    @ManyToOne(optional=false) @JoinColumn(name="locker_item_id")
    private LockerItem lockerItem;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private ActivityAction action;
    @Column(nullable=false)
    private LocalDateTime timestamp;
    public enum ActivityAction { TAKEN_FROM_LOCKER, KEPT_IN_LOCKER }
}
