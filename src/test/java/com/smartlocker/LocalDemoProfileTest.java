package com.smartlocker;

import com.smartlocker.repository.LockerItemRepository;
import com.smartlocker.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({"local", "demo"})
@TestPropertySource(properties = "app.jwt.secret=test-secret-that-is-long-enough-for-hmac-sha-256")
class LocalDemoProfileTest {
    @Autowired private StudentRepository students;
    @Autowired private LockerItemRepository items;

    @Test
    void localDemoProfileStartsWithoutMysqlOrGeminiAndLoadsSampleStudent() {
        var student = students.findByRollNumber("C7-101").orElseThrow();
        assertTrue(items.findByStudentId(student.getId()).size() >= 3);
    }
}
