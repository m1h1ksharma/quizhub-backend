package com.piet.quizhub.controller;

import com.piet.quizhub.entity.*;
import com.piet.quizhub.repository.*;
import com.piet.quizhub.security.JwtUtil;
import com.piet.quizhub.service.QuestionService;
import com.piet.quizhub.service.ResultService; 
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "http://localhost:3000")
public class StudentController {

    @Autowired private ResultRepository resultRepo;
    @Autowired private ResultService resultService; 
    @Autowired private JwtUtil jwtUtil;
    @Autowired private QuestionService questionService;
    @Autowired private QuizConfigRepository configRepo;

    private String getStudentNameFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try { return jwtUtil.extractName(authHeader.substring(7)); }
            catch (Exception e) { return "STUDENT"; }
        }
        return "STUDENT"; 
    }

    @GetMapping("/quiz-config")
    public ResponseEntity<?> getQuizConfig() {
        QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig(1L, "Normal_Quiz", 10));
        return ResponseEntity.ok(config);
    }

    @GetMapping("/check-status")
    public ResponseEntity<?> checkStatus(HttpServletRequest request, Authentication authentication) {
        try {
            String mobileNumber = authentication.getName();
            String studentName = getStudentNameFromToken(request);
            QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
            
            // Safety Check: Agar config null hai toh default check karo
            String activeRound = config.getActiveRound() != null ? config.getActiveRound() : "Normal_Quiz";

            Optional<Result> result = resultRepo.findByStudentNameAndStudentMobileAndQuizRound(
                studentName.trim(), mobileNumber.trim(), activeRound.trim()
            );

            return ResponseEntity.ok(Map.of(
                "attempted", result.isPresent(),
                "activeRound", activeRound
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("attempted", false));
        }
    }

    @GetMapping("/questions/{category}")
    public ResponseEntity<List<Question>> getStudentQuestions(@PathVariable String category) {
        try {
            QuizConfig config = configRepo.findById(1L).orElse(null);
            if (config == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

            // FIX: 403 se bachne ke liye comparison ko flexible rakho
            String currentLive = config.getActiveRound();
            
            // Agar category "playquiz" (frontend route) hai toh use current live round maano
            String targetCategory = (category.equalsIgnoreCase("playquiz")) ? currentLive : category;

            int limit = config.getQuestionLimit(); 
            List<Question> randomSet = questionService.getRandomQuestionsForStudent(targetCategory, limit);
            
            if (randomSet.isEmpty()) return ResponseEntity.noContent().build();

            return ResponseEntity.ok(randomSet);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody Map<String, String> answers, 
                                        HttpServletRequest request, 
                                        Authentication authentication) {
        try {
            String mobile = authentication.getName(); 
            String name = getStudentNameFromToken(request);
            QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
            
            List<Question> questions = questionService.getQuestionsByCategory(config.getActiveRound());
            int score = (int) questions.stream().filter(q -> 
                q.getCorrectAns().trim().equalsIgnoreCase(answers.getOrDefault(String.valueOf(q.getId()), "").trim())
            ).count();

            Result r = new Result();
            r.setStudentMobile(mobile);
            r.setStudentName(name);
            r.setScore(score);
            r.setTotalQuestions(questions.size());
            
            // 🔥 MARKS FIX: Config se question limit uthakar result mein save kar rahe hain
            r.setQuizLimit(config.getQuestionLimit()); 
            
            r.setQuizRound(config.getActiveRound());
            r.setAttempted(true);
            
            resultService.saveOrUpdateResult(r);
            return ResponseEntity.ok(Map.of("score", score, "attempted", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}