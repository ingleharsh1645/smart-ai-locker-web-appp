package com.smartlocker.repository;
import com.smartlocker.entity.LockerItem; import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LockerItemRepository extends JpaRepository<LockerItem,Long> { List<LockerItem> findByStudentId(Long studentId); }
