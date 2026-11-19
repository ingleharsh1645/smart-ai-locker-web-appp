package com.smartlocker.security;

import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service;
import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key; private final long expirationMs;
    public JwtService(@Value("${app.jwt.secret}") String secret,@Value("${app.jwt.expiration-ms}") long expirationMs){
        if(secret==null||secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        if(expirationMs<=0) throw new IllegalArgumentException("JWT_EXPIRATION_MS must be positive");
        this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationMs=expirationMs;
    }
    public String generate(String subject,String role,Long studentId){ return Jwts.builder().subject(subject).claim("role",role).claim("studentId",studentId).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+expirationMs)).signWith(key).compact(); }
    public Claims parse(String token){ return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
