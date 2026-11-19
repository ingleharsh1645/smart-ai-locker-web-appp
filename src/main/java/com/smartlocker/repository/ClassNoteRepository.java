package com.smartlocker.repository;

import com.smartlocker.entity.ClassNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ClassNoteRepository extends JpaRepository<ClassNote, Long> {
    List<ClassNote> findByDateGreaterThanEqualOrderByDateDesc(LocalDate date);
}
