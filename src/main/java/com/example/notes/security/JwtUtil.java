package com.example.notes.security;

import com.example.notes.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(User user) {
        // [OWASP A07/A08] token dlugozyjacy (24h), bez issuer/audience - celowo w wariancie bazowym
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .issuedAt(new Date(now))
                .expiration(new Date(now + 24 * 60 * 60 * 1000L))
                .signWith(key)
                .compact();
    }

    // [OWASP A08] walidacja ogranicza sie do podpisu i czasu zycia - bez issuer/audience
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
