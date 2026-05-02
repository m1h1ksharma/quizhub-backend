package com.piet.quizhub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final String SECRET_KEY =
            "PIET_QUIZHUB_SECRET_KEY_FOR_2026_EXAM_BATCH_SECURE_123456";

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // =========================
    // EXTRACT USERNAME (mobile)
    // =========================
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // =========================
    // EXTRACT ROLE
    // =========================
    public String extractRole(String token) {
        return extractClaim(token, claims -> (String) claims.get("role"));
    }

    // =========================
    // EXTRACT NAME
    // =========================
    public String extractName(String token) {
        return extractClaim(token, claims -> (String) claims.get("name"));
    }

    // =========================
    // GENERIC CLAIM EXTRACTOR
    // =========================
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return resolver.apply(claims);
    }

    // =========================
    // TOKEN GENERATION (FIXED)
    // =========================
    public String generateToken(String mobile, String role, String name) {

        Map<String, Object> claims = new HashMap<>();

        // 🔥 FIX: always store ROLE_ format
        String normalizedRole = role.toUpperCase();
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }

        claims.put("role", normalizedRole);
        claims.put("name", name);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(mobile)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24)) // 24h
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================
    // VALIDATE TOKEN
    // =========================
    public boolean validateToken(String token, String username) {
        return (extractUsername(token).equals(username) && !isTokenExpired(token));
    }

    // =========================
    // CHECK EXPIRATION
    // =========================
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}