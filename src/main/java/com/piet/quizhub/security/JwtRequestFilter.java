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

        // 1. Header aur "Bearer " prefix check karo
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            
            // 🔥 FORMAT CHECK: "2 period" error (invalid token) se bachne ke liye
            if (jwt != null && !jwt.isEmpty() && jwt.split("\\.").length == 3) {
                try {
                    username = jwtUtil.extractUsername(jwt);
                    role = jwtUtil.extractRole(jwt);
                } catch (Exception e) {
                    System.out.println("❌ JWT Error: " + e.getMessage());
                }
            } else {
                System.out.println("⚠️ Warning: Malformed or Empty Token received.");
            }
        }

        // 2. Agar username mila aur user already authenticated nahi hai
        if (username != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            try {
                if (jwtUtil.validateToken(jwt, username)) {
                    
                    // Spring Security expects roles with "ROLE_" prefix
                    String finalRole = role.toUpperCase().startsWith("ROLE_") ? 
                                       role.toUpperCase() : "ROLE_" + role.toUpperCase();

                    // Create Authentication Token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username, 
                            null, 
                            List.of(new SimpleGrantedAuthority(finalRole))
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 3. Set Security Context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    // Debugging ke liye terminal mein print hoga
                    System.out.println("✅ Authorized User: " + username + " | Final Role: " + finalRole);
                }
            } catch (Exception e) {
                System.out.println("❌ Token Validation Failed: " + e.getMessage());
            }
        }

        // 4. Filter chain ko aage badhao
        chain.doFilter(request, response);
    }
}