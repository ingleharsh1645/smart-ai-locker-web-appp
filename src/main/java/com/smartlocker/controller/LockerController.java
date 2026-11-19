package com.smartlocker.controller;
import com.smartlocker.dto.LockerDtos.*; import com.smartlocker.service.LockerService; import jakarta.validation.Valid; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/locker")
public class LockerController {
    private final LockerService service;
    public LockerController(LockerService service){this.service=service;}
    @GetMapping("/today")
    public SmartChecklist today(Authentication auth){return service.checklist(service.studentFromAuthentication(auth));}
    @PostMapping("/items")
    public ChecklistItem addItem(@Valid @RequestBody LockerItemRequest request,Authentication auth){
        return service.addItem(service.studentFromAuthentication(auth),request);
    }
    @PutMapping("/items/{itemId}")
    public ChecklistItem updateItem(@PathVariable Long itemId,@Valid @RequestBody LockerItemRequest request,Authentication auth){
        return service.updateItem(service.studentFromAuthentication(auth),itemId,request);
    }
    @GetMapping("/activity/today")
    public java.util.List<ActivityLog> activityToday(Authentication auth){
        return service.studentActivityToday(service.studentFromAuthentication(auth));
    }
    @PostMapping("/activity")
    public ActivityLog activity(@Valid @RequestBody ActivityRequest r,Authentication auth){return service.markActivity(service.studentFromAuthentication(auth),r);}
}
