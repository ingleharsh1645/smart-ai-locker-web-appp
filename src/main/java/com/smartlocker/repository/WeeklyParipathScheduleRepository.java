package com.smartlocker.repository;

import com.smartlocker.entity.WeeklyParipathSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Optional;

public interface WeeklyParipathScheduleRepository extends JpaRepository<WeeklyParipathSchedule, Long> {
    Optional<WeeklyParipathSchedule> findByDayOfWeek(DayOfWeek day);
}
