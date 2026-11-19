package com.smartlocker.repository;
import com.smartlocker.entity.TimetableEntry; import org.springframework.data.jpa.repository.JpaRepository;
import java.time.DayOfWeek; import java.util.List;
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry,Long> {
    List<TimetableEntry> findByDayOfWeekOrderByIdAsc(DayOfWeek day);
    List<TimetableEntry> findByDayOfWeek(DayOfWeek day);
    void deleteByDayOfWeek(DayOfWeek day);
}
