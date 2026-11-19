package com.smartlocker.controller;
import com.smartlocker.dto.LockerDtos.ParentMonitor; import com.smartlocker.service.LockerService; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat; import java.time.LocalDate; import java.time.ZoneId;
@RestController @RequestMapping("/api/parent")
public class ParentController {
    private final LockerService service;
    public ParentController(LockerService service){this.service=service;}
    @GetMapping("/monitor")
    public ParentMonitor monitor(Authentication auth,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){
        LocalDate reportDate=date==null?LocalDate.now(ZoneId.of("Asia/Kolkata")):date;
        return service.monitor(service.studentByIdForParent(auth),reportDate);
    }
}
