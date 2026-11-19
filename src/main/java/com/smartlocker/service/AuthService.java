package com.smartlocker.service;

import com.smartlocker.dto.AuthDtos.*; import com.smartlocker.entity.*; import com.smartlocker.exception.ApiException; import com.smartlocker.repository.*; import com.smartlocker.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthService {
    private final StudentRepository students; private final UserAccountRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    @Value("${app.teacher.access-key:}")
    private String teacherAccessKey;
    public AuthService(StudentRepository students,UserAccountRepository users,PasswordEncoder encoder,JwtService jwt){this.students=students;this.users=users;this.encoder=encoder;this.jwt=jwt;}
    @Transactional(readOnly=true)
    public AuthResponse studentLogin(StudentLoginRequest r){ Student s=students.findByRollNumber(r.rollNumber().trim()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Invalid student credentials")); if(!"7".equals(s.getStandard())||!encoder.matches(r.specialPassword(),s.getSpecialPassword())) throw new ApiException(HttpStatus.UNAUTHORIZED,"Invalid student credentials"); return new AuthResponse(jwt.generate(s.getRollNumber(),"STUDENT",s.getId()),Role.STUDENT,s.getName(),s.getId()); }
    @Transactional
    public AuthResponse parentRegister(ParentRegisterRequest r){
        String email=r.email()==null?null:r.email().trim().toLowerCase(java.util.Locale.ROOT);
        String mobile=normalizeMobile(r.mobile());
        if(email!=null&&email.isBlank()) email=null;
        if(email==null&&mobile==null) throw new ApiException(HttpStatus.BAD_REQUEST,"Email or mobile is required");
        if(email!=null&&users.findByEmail(email).isPresent()) throw new ApiException(HttpStatus.CONFLICT,"Email already registered");
        if(mobile!=null&&users.findByMobile(mobile).isPresent()) throw new ApiException(HttpStatus.CONFLICT,"Mobile already registered");
        Student s=students.findByRollNumber(r.rollNumber().trim()).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"Child roll number not found"));
        if(!"7".equals(s.getStandard())||!encoder.matches(r.specialPassword(),s.getSpecialPassword())) throw new ApiException(HttpStatus.BAD_REQUEST,"Invalid child credentials");
        UserAccount u=UserAccount.builder().email(email).mobile(mobile).password(encoder.encode(r.password())).role(Role.PARENT).student(s).build();
        users.save(u);
        return new AuthResponse(jwt.generate(email!=null?email:mobile,"PARENT",s.getId()),Role.PARENT,s.getName(),s.getId());
    }
    @Transactional(readOnly=true)
    public AuthResponse parentLogin(ParentLoginRequest r){
        String identity=r.identity().trim();
        boolean emailIdentity=identity.contains("@");
        String normalizedIdentity=emailIdentity?identity.toLowerCase(java.util.Locale.ROOT):normalizeMobile(identity);
        UserAccount u=(emailIdentity?users.findByEmail(normalizedIdentity).orElse(null):users.findByMobile(normalizedIdentity).orElse(null));
        if(u==null||u.getRole()!=Role.PARENT||!encoder.matches(r.password(),u.getPassword())) throw new ApiException(HttpStatus.UNAUTHORIZED,"Invalid parent credentials");
        return new AuthResponse(jwt.generate(normalizedIdentity,"PARENT",u.getStudent().getId()),Role.PARENT,u.getStudent().getName(),u.getStudent().getId());
    }

    public AuthResponse teacherLogin(TeacherLoginRequest request) {
        if (teacherAccessKey == null || teacherAccessKey.length() < 32) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "School admin login is not configured. Set TEACHER_ACCESS_KEY to a secret with at least 32 characters.");
        }
        boolean matches = MessageDigest.isEqual(
                teacherAccessKey.getBytes(StandardCharsets.UTF_8),
                request.accessKey().getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid school admin access key");
        }
        return new AuthResponse(jwt.generate("class7-teacher", Role.TEACHER.name(), null),
                Role.TEACHER, "Class 7 Teacher", null);
    }

    private String normalizeMobile(String mobile){
        if(mobile==null)return null;
        String normalized=mobile.replaceAll("[\\s().-]","");
        if(normalized.isEmpty())return null;
        if(!normalized.matches("\\+?[0-9]{10,15}")) throw new ApiException(HttpStatus.BAD_REQUEST,"Mobile must contain 10 to 15 digits");
        return normalized;
    }
}
