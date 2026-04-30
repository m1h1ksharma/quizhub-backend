package com.piet.quizhub.config;

import com.piet.quizhub.security.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults()) // CORS enable karna zaroori hai
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 1. Preflight requests (CORS OPTIONS) allow karo
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 2. Public Endpoints (Login/Register)
                // Yahan /auth/** aur /api/auth/** dono allow hain safety ke liye
                .requestMatchers("/api/auth/**", "/auth/**").permitAll()
                
                // 3. Student Endpoints
                .requestMatchers("/api/student/**").hasAnyAuthority("ROLE_STUDENT", "ROLE_ADMIN")
                
                // 4. Admin Endpoints
                .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                
                // 5. Baki sab secure
                .anyRequest().authenticated()
            );

        // JWT filter ko UsernamePassword filter se pehle lagao
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        
        // Yahan tumhare frontend ke links honge
        cfg.setAllowedOrigins(List.of(
            "http://localhost:3000", 
            "http://127.0.0.1:3000",
            "https://quizhub-frontend-six.vercel.app" // Tera actual Vercel link
        ));
        
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept")); 
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true); // Cookies/Auth headers ke liye true
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}