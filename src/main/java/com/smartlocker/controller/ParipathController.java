package com.smartlocker.controller;

import com.smartlocker.dto.ParipathDtos.ParipathResponse;
import com.smartlocker.service.ParipathService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/class7/paripath")
public class ParipathController {
    private final ParipathService paripathService;

    public ParipathController(ParipathService paripathService) {
        this.paripathService = paripathService;
    }

    @GetMapping("/today")
    public ParipathResponse today() {
        return paripathService.today();
    }
}
