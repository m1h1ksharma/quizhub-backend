package com.piet.quizhub.controller;

import com.piet.quizhub.entity.PasswordResetToken;
import com.piet.quizhub.entity.User;
import com.piet.quizhub.repository.ResetTokenRepository;
import com.piet.quizhub.repository.UserRepository;
import com.piet.quizhub.security.JwtUtil;
import com.piet.quizhub.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "https://quizhub-frontend-six.vercel.app"})
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private EmailService emailService;
    @Autowired private ResetTokenRepository resetTokenRepo;

    // --- 1. REGISTER ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            String name = user.getName().trim();
            String mobile = user.getMobileNumber().trim();

            if (userRepository.existsByMobileNumberAndName(mobile, name)) {
                return errorResponse(HttpStatus.CONFLICT, "Student already registered on this mobile!");
            }

            user.setName(name);
            user.setMobileNumber(mobile);
            user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
            user.setRole("STUDENT");
            userRepository.save(user);

            String token = jwtUtil.generateToken(mobile, "STUDENT", name);
            return ResponseEntity.ok(Map.of("token", token, "name", name, "message", "Registration Successful!"));
        } catch (Exception e) {
            return errorResponse(HttpStatus.BAD_REQUEST, "Registration failed!");
        }
    }

    // --- 2. CHECK MOBILE (Sibling Management) ---
    @PostMapping("/check-mobile")
    public ResponseEntity<?> checkMobile(@RequestBody Map<String, String> request) {
        String mobile = request.getOrDefault("mobileNumber", "").trim();
        List<User> users = userRepository.findAllByMobileNumber(mobile);

        if (users.isEmpty()) {
            return errorResponse(HttpStatus.NOT_FOUND, "Mobile number not registered!");
        }

        List<String> names = users.stream().map(User::getName).collect(Collectors.toList());
        return ResponseEntity.ok(names);
    }

    // --- 3. LOGIN (Student & Admin) ---
    @PostMapping("/login-student")
    public ResponseEntity<?> loginStudent(@RequestBody Map<String, String> request) {
        return processLogin(request.get("mobileNumber"), request.get("name"), request.get("password"), false);
    }

    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@RequestBody Map<String, String> request) {
        return processLogin(request.get("email"), null, request.get("password"), true);
    }

    // --- 4. FORGOT PASSWORD REQUEST ---
    @RequestMapping(value = {"/forgot-password/request", "/send-otp"}, method = RequestMethod.POST)
    public ResponseEntity<?> handleRecoveryRequest(@RequestBody Map<String, String> request) {
        String type = request.getOrDefault("type", "MOBILE"); 
        String identifier = request.getOrDefault("identifier", "").trim();
        String name = request.getOrDefault("name", "").trim();

        Optional<User> userOpt = "EMAIL".equalsIgnoreCase(type) 
                ? userRepository.findByEmail(identifier) 
                : userRepository.findByMobileNumberAndName(identifier, name);

        if (userOpt.isPresent()) {
            String secret = "EMAIL".equalsIgnoreCase(type) 
                ? UUID.randomUUID().toString() 
                : String.valueOf(new Random().nextInt(900000) + 100000);
            
            saveToken(secret, "EMAIL".equalsIgnoreCase(type) ? identifier : null, 
                      "MOBILE".equalsIgnoreCase(type) ? identifier + ":" + name : null);
            
            if ("EMAIL".equalsIgnoreCase(type)) {
                emailService.sendResetLink(identifier, secret);
            } else {
                System.out.println("DEBUG OTP for " + userOpt.get().getName() + ": " + secret);
            }

            return ResponseEntity.ok(Map.of("message", "Recovery process initiated!"));
        }
        return errorResponse(HttpStatus.NOT_FOUND, "User details not found!");
    }

    // --- 5. RESET PASSWORD (CRITICAL FIX FOR NPE) ---
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            // SAFE EXTRACTION: No more .trim() on null values
            String tokenOrOtp = request.containsKey("tokenOrOtp") ? request.get("tokenOrOtp").trim() : "";
            String newPassword = request.containsKey("newPassword") ? request.get("newPassword").trim() : "";
            String identifier = request.containsKey("identifier") ? request.get("identifier").trim() : "";
            String name = request.containsKey("name") && request.get("name") != null ? request.get("name").trim() : "";

            if (tokenOrOtp.isEmpty() || newPassword.isEmpty() || identifier.isEmpty()) {
                return errorResponse(HttpStatus.BAD_REQUEST, "Missing required data (Token, Password, or Identifier)!");
            }

            // Verify Token
            Optional<PasswordResetToken> tokenOpt = resetTokenRepo.findByToken(tokenOrOtp);
            if (tokenOpt.isEmpty()) {
                return errorResponse(HttpStatus.BAD_REQUEST, "Invalid recovery code!");
            }

            PasswordResetToken resetToken = tokenOpt.get();
            if (resetToken.isExpired()) {
                resetTokenRepo.delete(resetToken);
                return errorResponse(HttpStatus.BAD_REQUEST, "Code expired!");
            }

            // Find User Safely
            Optional<User> userOpt = (resetToken.getEmail() != null)
                    ? userRepository.findByEmail(resetToken.getEmail())
                    : userRepository.findByMobileNumberAndName(identifier, name);

            if (userOpt.isEmpty()) {
                return errorResponse(HttpStatus.NOT_FOUND, "User details mismatch!");
            }

            // Success: Update & Cleanup
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            resetTokenRepo.delete(resetToken); 

            return ResponseEntity.ok(Map.of("message", "Password updated successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Server Error: " + e.getMessage()));
        }
    }

    // --- HELPERS ---
    private ResponseEntity<?> processLogin(String id, String name, String pass, boolean isAdmin) {
        if (id == null || pass == null) return errorResponse(HttpStatus.BAD_REQUEST, "Missing fields!");
        
        Optional<User> userOpt = isAdmin ? userRepository.findByEmail(id) : userRepository.findByMobileNumberAndName(id, name);
        
        if (userOpt.isPresent() && passwordEncoder.matches(pass, userOpt.get().getPassword())) {
            User u = userOpt.get();
            if (isAdmin && !"ADMIN".equals(u.getRole())) return errorResponse(HttpStatus.FORBIDDEN, "Access Denied!");
            
            String token = jwtUtil.generateToken(u.getMobileNumber(), u.getRole(), u.getName());
            return ResponseEntity.ok(Map.of("token", token, "role", u.getRole(), "name", u.getName()));
        }
        return errorResponse(HttpStatus.UNAUTHORIZED, "Invalid Credentials!");
    }

    private void saveToken(String token, String email, String mobileWithCombo) {
        PasswordResetToken t = new PasswordResetToken();
        t.setToken(token);
        t.setEmail(email);
        t.setMobileNumber(mobileWithCombo);
        t.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        resetTokenRepo.save(t);
    }

    private ResponseEntity<?> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}