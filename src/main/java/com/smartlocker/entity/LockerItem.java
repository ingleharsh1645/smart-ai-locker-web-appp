package com.smartlocker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="locker_items", uniqueConstraints=@UniqueConstraint(columnNames={"student_id","name"})) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LockerItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="student_id")
    private Student student;
    @Column(nullable=false)
    private String name;
    @Column(nullable=false)
    private String category;
    @Column(nullable=false)
    private String subject;
    private boolean requiredToday;
}
