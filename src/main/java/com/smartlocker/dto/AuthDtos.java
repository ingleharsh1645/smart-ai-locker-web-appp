package com.smartlocker.dto;

import com.smartlocker.entity.Role;
import jakarta.validation.constraints.*;

public class AuthDtos {
    public record StudentLoginRequest(@NotBlank String rollNumber,@NotBlank String specialPassword) {}
    public record ParentRegisterRequest(
            @Pattern(regexp="^(?:\\+?[0-9][0-9\\s().-]{8,20}[0-9])?$",message="Enter a mobile number with 10 to 15 digits")
            String mobile,
            @Email String email,
            @NotBlank @Size(min=8,max=72) String password,
            @NotBlank String rollNumber,
            @NotBlank String specialPassword) {}
    public record ParentLoginRequest(@NotBlank String identity,@NotBlank String password) {}
    public record TeacherLoginRequest(@NotBlank @Size(max=256) String accessKey) {}
    public record AuthResponse(String token,Role role,String name,Long studentId) {}
}
