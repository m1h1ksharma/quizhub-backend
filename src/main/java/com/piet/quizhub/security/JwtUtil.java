package com.piet.quizhub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.*;
import java.util.function.Function;

@Component
public class JwtUtil {
    private final String SECRET_KEY = "PIET_QUIZHUB_SECRET_KEY_FOR_2026_EXAM_BATCH_SECURE";
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    public String extractName(String token) { 
        return extractClaim(token, claims -> (String) claims.get("name")); 
    }

    public String extractUsername(String token) { 
        return extractClaim(token, Claims::getSubject); 
    }

    public String extractRole(String token) { 
        return extractClaim(token, claims -> (String) claims.get("role")); 
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claimsResolver.apply(claims);
    }

    public String generateToken(String mobile, String role, String name) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("name", name);
        return Jwts.builder()
                .setClaims(claims).setSubject(mobile)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24)) // 24 hours
                .signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public Boolean validateToken(String token, String username) {
        return (extractUsername(token).equals(username) && !isTokenExpired(token));
    }

    private Boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}