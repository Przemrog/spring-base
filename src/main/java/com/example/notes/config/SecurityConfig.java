package com.example.notes.config;

import com.example.notes.security.JwtAuthFilter;
import com.example.notes.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    // Warstwa /api - bezstanowa, uwierzytelnianie tokenem JWT.
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http, JwtUtil jwtUtil) throws Exception {
        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtUtil);
        http
            .securityMatcher("/api/**")
            // API oparte na tokenie Bearer - CSRF ciasteczkowy nie dotyczy warstwy bezstanowej
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/login").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Warstwa webowa - formularz logowania. Spring Security domyslnie wlacza CSRF
    // oraz podstawowe naglowki (X-Frame-Options, X-Content-Type-Options).
    @Bean
    @Order(2)
    public SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/register", "/login", "/css/**", "/avatars/**").permitAll()
                // [OWASP A01 - eskalacja pionowa] /admin dostepny dla KAZDEGO zalogowanego,
                // bez wymogu roli ADMIN - celowo w wariancie bazowym.
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/notes", true)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login"));
        // [OWASP A02] brak jawnej polityki CSP - dodamy ja dopiero w wariancie utwardzonym.
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // [OWASP A04] haszowanie BCrypt - wbudowane, bezpieczne domyslnie
        return new BCryptPasswordEncoder();
    }
}
