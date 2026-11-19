package com.smartlocker.controller;
import com.smartlocker.service.AiChatService; import jakarta.validation.Valid; import jakarta.validation.constraints.NotBlank; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/ai")
public class AiController { private final AiChatService service; public AiController(AiChatService service){this.service=service;} public record ChatRequest(@NotBlank String message){} public record ChatResponse(String reply){} @PostMapping("/chat") public ChatResponse chat(@Valid @RequestBody ChatRequest r,Authentication auth){return new ChatResponse(service.chat(auth,r.message()));} }
