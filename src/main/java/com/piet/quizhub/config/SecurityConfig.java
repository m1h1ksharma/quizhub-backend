package com.piet.quizhub.config;

import com.piet.quizhub.security.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
            // 1. CSRF disable karna zaroori hai multipart/POST requests ke liye
            .csrf(csrf -> csrf.disable()) 
            
            // 2. Global CORS configuration apply karna
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 3. Stateless session management (JWT ke liye)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            .authorizeHttpRequests(auth -> auth
                // 4. Preflight OPTIONS requests ko hamesha allow karo
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 5. Public endpoints (Login/Register)
                .requestMatchers("/api/auth/**", "/auth/**").permitAll()
                
                // 6. Admin Endpoints: Dono variants (ROLE_ADMIN aur ADMIN) allow kiye hain
                .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                
                // 7. Student Endpoints
                .requestMatchers("/api/student/**").hasAnyAuthority("ROLE_STUDENT", "STUDENT", "ROLE_ADMIN", "ADMIN")
                
                // 8. Baki sab secure
                .anyRequest().authenticated()
            );

        // 9. JWT Filter ko UsernamePassword filter se pehle add karna
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        
        // Frontend origins jo allowed hain
        cfg.setAllowedOrigins(List.of(
            "http://localhost:3000", 
            "https://quizhub-frontend-six.vercel.app"
        ));
        
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Headers jo communication ke liye zaroori hain
        cfg.setAllowedHeaders(List.of(
            "Authorization", 
            "Content-Type", 
            "Accept", 
            "X-Requested-With", 
            "Origin"
        ));
        
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean 
    public BCryptPasswordEncoder passwordEncoder() { 
        return new BCryptPasswordEncoder(); 
    }
}