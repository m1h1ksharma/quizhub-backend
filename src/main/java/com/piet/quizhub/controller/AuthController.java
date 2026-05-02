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
@CrossOrigin(origins = "https://quizhub-frontend-six.vercel.app")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // =========================
    // REGISTER STUDENT
    // =========================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        if (userRepository.existsByMobileNumberAndName(
                user.getMobileNumber(),
                user.getName())) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Already registered. Please login."));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // FIXED: consistent role format
        user.setRole("ROLE_STUDENT");

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
                "message", "Registration Successful"
        ));
    }

    // =========================
    // CHECK MOBILE
    // =========================
    @PostMapping("/check-mobile")
    public ResponseEntity<?> checkMobile(@RequestBody Map<String, String> request) {

        String mobile = request.get("mobileNumber");
        List<User> users = userRepository.findAllByMobileNumber(mobile);

        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Not registered");
        }

        List<String> names = users.stream()
                .map(User::getName)
                .collect(Collectors.toList());

        return ResponseEntity.ok(names);
    }

    // =========================
    // STUDENT LOGIN
    // =========================
    @PostMapping("/login-student")
    public ResponseEntity<?> loginStudent(@RequestBody Map<String, String> request) {

        String mobile = request.get("mobileNumber");
        String name = request.get("name");
        String password = request.get("password");

        Optional<User> userOpt =
                userRepository.findByMobileNumberAndName(mobile, name);

        if (userOpt.isPresent()) {

            User user = userOpt.get();

            if (passwordEncoder.matches(password, user.getPassword())) {

                String token = jwtUtil.generateToken(
                        user.getMobileNumber(),
                        user.getRole(),
                        user.getName()
                );

                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "role", user.getRole(),
                        "name", user.getName()
                ));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid Password");
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("User not found");
    }

    // =========================
    // ADMIN LOGIN
    // =========================
    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String password = request.get("password");

        Optional<User> adminOpt = userRepository.findByEmail(email);

        if (adminOpt.isPresent()) {

            User admin = adminOpt.get();

            if ("ROLE_ADMIN".equals(admin.getRole())
                    && passwordEncoder.matches(password, admin.getPassword())) {

                String token = jwtUtil.generateToken(
                        admin.getMobileNumber(),
                        admin.getRole(),
                        admin.getName()
                );

                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "role", admin.getRole(),
                        "name", admin.getName()
                ));
            }
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Unauthorized Admin Login");
    }
}