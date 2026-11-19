package com.smartlocker.controller;

import com.smartlocker.dto.ParipathDtos.ParipathResponse;
import com.smartlocker.dto.ParipathDtos.ScheduleRequest;
import com.smartlocker.service.ParipathService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/paripath")
public class TeacherParipathController {
    private final ParipathService paripathService;

    public TeacherParipathController(ParipathService paripathService) {
        this.paripathService = paripathService;
    }

    @GetMapping
    public List<ParipathResponse> upcoming() {
        return paripathService.upcoming();
    }

    @PutMapping("/{date}")
    public ParipathResponse save(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody ScheduleRequest request) {
        return paripathService.save(date, request);
    }
}
