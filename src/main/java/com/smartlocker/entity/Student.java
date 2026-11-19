package com.smartlocker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="students") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="roll_number", nullable=false, unique=true)
    private String rollNumber;
    @Column(nullable=false)
    private String name;
    @Column(nullable=false)
    @Builder.Default
    private String standard = "7";
    @Column(name="special_password", nullable=false)
    private String specialPassword;
}
