package com.piet.quizhub.controller;

import com.piet.quizhub.entity.User;
import com.piet.quizhub.repository.UserRepository;
import com.piet.quizhub.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000") 
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // ================= REGISTER =================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.existsByMobileNumberAndName(user.getMobileNumber(), user.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Already registered with this name! Please login.");
        }
        
        // Password encrypt karke save karo
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("STUDENT");
        userRepository.save(user);

        String token = jwtUtil.generateToken(
                user.getMobileNumber(),
                user.getRole(),
                user.getName()
        );
        
        return ResponseEntity.ok(Map.of(
            "token", token, 
            "role", user.getRole(),
            "name", user.getName(),
            "message", "Registration Successful!"
        ));
    }

    // ================= CHECK MOBILE =================
    @PostMapping("/check-mobile")
    public ResponseEntity<?> checkMobile(@RequestBody Map<String, String> request) {
        String mobile = request.get("mobileNumber");
        List<User> users = userRepository.findAllByMobileNumber(mobile);
        
        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not registered!");
        }
        
        List<String> names = users.stream().map(User::getName).collect(Collectors.toList());
        return ResponseEntity.ok(names);
    }

    // ================= STUDENT LOGIN (FIXED) =================
    @PostMapping("/login-student")
    public ResponseEntity<?> loginStudent(@RequestBody Map<String, String> request) {
        String mobile = request.get("mobileNumber");
        
        String name = request.get("name"); 
        String password = request.get("password");

        
        System.out.println("Login Attempt -> Mobile: " + mobile + " | Name: " + name);

        Optional<User> userOpt = userRepository.findByMobileNumberAndName(mobile, name);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Password match check
            if (passwordEncoder.matches(password, user.getPassword())) {

                String token = jwtUtil.generateToken(
                        user.getMobileNumber(),
                        user.getRole(),
                        user.getName()
                );
                
                System.out.println("✅ Login Success for: " + name);
                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "role", user.getRole(),
                        "name", user.getName()
                )); 
            } else {
                System.out.println("❌ Password Mismatch for: " + name);
            }
        } else {
            System.out.println("❌ User not found in DB for Mobile: " + mobile + " and Name: " + name);
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Password or Name!");
    }

    // ================= ADMIN LOGIN =================
    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        Optional<User> adminOpt = userRepository.findByEmail(email);

        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            if ("ADMIN".equals(admin.getRole()) && passwordEncoder.matches(password, admin.getPassword())) {

                String token = jwtUtil.generateToken(
                        admin.getMobileNumber(),
                        "ADMIN",
                        admin.getName()
                );

                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "role", "ADMIN",
                        "name", admin.getName()
                ));
            }
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized Admin Login!");
    }
}