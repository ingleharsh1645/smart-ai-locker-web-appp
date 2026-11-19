package com.smartlocker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="user_accounts") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(unique=true)
    private String email;
    @Column(unique=true)
    private String mobile;
    @Column(nullable=false)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Role role;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="student_id")
    private Student student;
}
