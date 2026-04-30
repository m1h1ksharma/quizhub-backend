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
@RequestMapping("/api/student") // Base path sync kiya gaya hai
@CrossOrigin(origins = "https://quizhub-frontend-six.vercel.app")
public class StudentController {

    @Autowired private ResultRepository resultRepo;
    @Autowired private ResultService resultService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private QuestionService questionService;
    @Autowired private QuizConfigRepository configRepo;

    // Token se student name nikalne ka helper function
    private String getStudentNameFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractName(authHeader.substring(7));
            } catch (Exception e) {
                return "STUDENT";
            }
        }
        return "STUDENT";
    }

    @GetMapping("/quiz-config")
    public ResponseEntity<?> getQuizConfig() {
        QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig(1L, "Normal Quiz", 10));
        return ResponseEntity.ok(config);
    }

    @GetMapping("/check-status")
    public ResponseEntity<?> checkStatus(HttpServletRequest request, Authentication authentication) {
        try {
            String mobileNumber = authentication.getName();
            String studentName = getStudentNameFromToken(request);
            
            QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
            String activeRound = config.getActiveRound() != null ? config.getActiveRound() : "Normal_Quiz";

            // Check if student already attempted this round
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

            // Flexible category matching
            String currentLive = config.getActiveRound();
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
    public ResponseEntity<?> submitQuiz(@RequestBody Map<String, String> answers, HttpServletRequest request, Authentication authentication) {
        try {
            String mobile = authentication.getName();
            String name = getStudentNameFromToken(request);
            
            QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
            List<Question> questions = questionService.getQuestionsByCategory(config.getActiveRound());

            // Scoring logic[cite: 4]
            int score = (int) questions.stream().filter(q -> 
                q.getCorrectAns().trim().equalsIgnoreCase(answers.getOrDefault(String.valueOf(q.getId()), "").trim())
            ).count();

            Result r = new Result();
            r.setStudentMobile(mobile);
            r.setStudentName(name);
            r.setScore(score);
            r.setTotalQuestions(questions.size());
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