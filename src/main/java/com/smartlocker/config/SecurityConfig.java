package com.smartlocker.config;

import com.smartlocker.security.JwtAuthenticationFilter; import org.springframework.context.annotation.*; import org.springframework.http.HttpStatus; import org.springframework.security.config.Customizer; import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.config.http.SessionCreationPolicy; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.SecurityFilterChain; import org.springframework.security.web.authentication.HttpStatusEntryPoint; import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    public SecurityConfig(JwtAuthenticationFilter jwtFilter){this.jwtFilter=jwtFilter;}
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf->csrf.disable()).cors(Customizer.withDefaults()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).exceptionHandling(e->e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))).authorizeHttpRequests(a->a.requestMatchers("/","/index.html","/manifest.webmanifest","/sw.js","/registerSW.js","/workbox-*.js","/assets/**","/locker-icon.svg","/api/auth/**","/api/class7/daily-info","/api/class7/paripath/today","/api/class7/portal-addresses","/error").permitAll().requestMatchers("/api/teacher/**").hasRole("TEACHER").requestMatchers("/api/ai/**").authenticated().requestMatchers("/api/parent/**").hasRole("PARENT").requestMatchers("/api/locker/**","/api/class7/**").hasRole("STUDENT").anyRequest().authenticated()).addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class).build();
    }
}
