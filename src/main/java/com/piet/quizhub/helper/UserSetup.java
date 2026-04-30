package com.piet.quizhub.helper;

import com.piet.quizhub.entity.User;
import com.piet.quizhub.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserSetup implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Interface use karna better practice hai

    public UserSetup(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "admin@piet.co.in"; 
        
        // 1. Check if Admin already exists by Email
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setName("ADMIN"); // Professional look ke liye caps
            admin.setFatherName("PIET MANAGEMENT");
            admin.setMobileNumber("0000000000"); // Admin ke liye default mobile
            admin.setEmail(adminEmail);
            
            // Encode the password
            admin.setPassword(passwordEncoder.encode("admin123")); 
            
            admin.setRole("ADMIN");

            

            userRepository.save(admin);
            System.out.println("✅ SUCCESS: Admin User Created (admin@piet.co.in / admin123)");
        } else {
            System.out.println("ℹ️ INFO: Admin User already exists in database.");
        }
    }
}