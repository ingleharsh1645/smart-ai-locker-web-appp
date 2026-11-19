package com.smartlocker.repository;
import com.smartlocker.entity.LockerActivity; import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime; import java.util.List;
public interface LockerActivityRepository extends JpaRepository<LockerActivity,Long> { List<LockerActivity> findByStudentIdOrderByTimestampDesc(Long studentId); List<LockerActivity> findByStudentIdAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampDesc(Long studentId,LocalDateTime from,LocalDateTime to); }
