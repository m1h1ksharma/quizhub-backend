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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String role = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            
            if (jwt != null && !jwt.isEmpty() && jwt.split("\\.").length == 3) {
                try {
                    username = jwtUtil.extractUsername(jwt);
                    role = jwtUtil.extractRole(jwt);
                } catch (Exception e) {
                    System.out.println("JWT Error: " + e.getMessage());
                }
            } else {
                System.out.println("Warning: Malformed or Empty Token received.");
            }
        }

        if (username != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (jwtUtil.validateToken(jwt, username)) {
    // Role normalize karo
    String cleanRole = role.toUpperCase().replace("ROLE_", "");
    String roleWithPrefix = "ROLE_" + cleanRole;

    List<SimpleGrantedAuthority> authorities = List.of(
        new SimpleGrantedAuthority(roleWithPrefix),
        new SimpleGrantedAuthority(cleanRole)
    );

    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            username, null, authorities);

    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

    SecurityContextHolder.getContext().setAuthentication(authToken);
    System.out.println("Authorized User: " + username + " | Granted Authorities: " + authorities);
}
            } catch (Exception e) {
                System.out.println("Token Validation Failed: " + e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}