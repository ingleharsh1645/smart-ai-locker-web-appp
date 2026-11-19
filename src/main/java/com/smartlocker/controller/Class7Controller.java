package com.smartlocker.controller;
import com.smartlocker.service.DailyInfoService; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/class7")
public class Class7Controller { private final DailyInfoService service; public Class7Controller(DailyInfoService service){this.service=service;} @GetMapping("/daily-info") public java.util.Map<String,Object> daily(){return service.today();} }
