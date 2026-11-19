package com.smartlocker.repository;

import com.smartlocker.entity.ParipathSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ParipathScheduleRepository extends JpaRepository<ParipathSchedule, Long> {
    Optional<ParipathSchedule> findByDate(LocalDate date);
    List<ParipathSchedule> findByDateGreaterThanEqualOrderByDateAsc(LocalDate date);
}
