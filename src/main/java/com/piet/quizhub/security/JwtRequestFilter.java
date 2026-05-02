package com.piet.quizhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        String username = null;
        String role = null;
        String token = null;

        // =========================
        // STEP 1: Extract token
        // =========================
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);

            try {
                username = jwtUtil.extractUsername(token);
                role = jwtUtil.extractRole(token);
            } catch (Exception e) {
                System.out.println("JWT parsing failed: " + e.getMessage());
            }
        }

        // =========================
        // STEP 2: Set authentication
        // =========================
        if (username != null
                && role != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                if (jwtUtil.validateToken(token, username)) {

                    // role already normalized in JwtUtil (ROLE_ADMIN / ROLE_STUDENT)
                    SimpleGrantedAuthority authority =
                            new SimpleGrantedAuthority(role);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    List.of(authority)
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println("Authenticated User: " + username +
                            " | Role: " + role);
                }

            } catch (Exception e) {
                System.out.println("Authentication failed: " + e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}