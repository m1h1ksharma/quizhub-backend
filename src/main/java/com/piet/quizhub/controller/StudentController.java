
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "https://quizhub-frontend-six.vercel.app")

// @CrossOrigin(origins = "http://localhost:3000") // Frontend port sync
public class StudentController {

    @Autowired private ResultRepository resultRepo;
    @Autowired private ResultService resultService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private QuestionService questionService;
    @Autowired private QuizConfigRepository configRepo;
    @Autowired private QuizSettingsRepository quizSettingsRepository;
    @Autowired private UserRepository userRepo;

    // Helper: Token se student name extract karne ke liye
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

    // 1. Get Quiz Configuration (Timer, Round Name)
    @GetMapping("/quiz-config")
    public ResponseEntity<?> getQuizConfig() {
        QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig(1L, "Normal Quiz", 10));
        return ResponseEntity.ok(config);
    }

    // 2. Check if student already attempted the quiz
@GetMapping("/check-status")
public ResponseEntity<?> checkStatus(HttpServletRequest request, Authentication authentication) {
    try {
        // 1. Student ki identity (Token se mobile aur name uthao)
        String mobileNumber = authentication.getName();
        String studentName = getStudentNameFromToken(request);
        
        // 2. Sirf Active Round pata karne ke liye QuizConfig uthao
        QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
        String activeRound = (config.getActiveRound() != null) ? config.getActiveRound() : "Normal_Quiz";

        // 3. Us Active Round ki settings (Toggle status) nikaalo
        QuizSettings roundSettings = quizSettingsRepository.findById(activeRound)
                .orElse(new QuizSettings(activeRound, 10, false, false));

        // 4. Check karo kya student ne ye round pehle hi submit kar diya hai
        Optional<Result> result = resultRepo.findByStudentNameAndStudentMobileAndQuizRound(
            studentName.trim(), mobileNumber.trim(), activeRound.trim()
        );

        Map<String, Object> response = new HashMap<>();
        boolean isAttempted = result.isPresent(); // Isse Dashboard 'Submitted' dikhayega
        
        response.put("attempted", isAttempted);
        response.put("round", activeRound);
        
        // Mapping to QuizSettings Toggle
        boolean adminAllowedMarks = roundSettings.isShowResult(); 
        response.put("showMarks", adminAllowedMarks);

        // 5. Agar Quiz submitted hai AUR Admin ne result publish kar diya hai
        if (isAttempted && adminAllowedMarks) {
            response.put("score", result.get().getScore());
            response.put("total", result.get().getTotalQuestions());
        }

        // =====================================================================
        // 🎯 EXACT PROPERTY MATCH: Linked to User Entity Fields
        // =====================================================================
        Map<String, Object> studentData = new HashMap<>();

        // 1. Database combinations lookup using your UserRepository
        Optional<User> dbUser = userRepo.findByMobileNumberAndName(mobileNumber.trim(), studentName.trim());

        if (dbUser.isPresent()) {
            User user = dbUser.get();
            
            // ✅ Exact mapped properties from your User.java entity
            studentData.put("studentName", user.getName());
            studentData.put("studentMobile", user.getMobileNumber());
            studentData.put("fatherName", user.getFatherName() != null ? user.getFatherName() : "N/A");
            studentData.put("emailId", user.getEmail() != null ? user.getEmail() : "N/A");
            
            // 🔥 FIXED MATCH: course ki jagah teri entity ka strict field 'classLevel' map hoga
            studentData.put("className", user.getClassLevel() != null ? user.getClassLevel() : "N/A");
            
            studentData.put("id", user.getId());
        } else {
            // Fallback framework block if memory trace fails
            studentData.put("studentName", studentName.trim());
            studentData.put("studentMobile", mobileNumber.trim());
            studentData.put("className", "N/A");
            studentData.put("fatherName", "N/A");
            studentData.put("emailId", "N/A");
            studentData.put("id", "AUTH_LIVE");
        }

        response.put("student", studentData);
        return ResponseEntity.ok(response);

    } catch (Exception e) {
        // Fallback safe response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("attempted", false);
        errorResponse.put("round", "Unknown");
        errorResponse.put("showMarks", false);
        return ResponseEntity.ok(errorResponse);
    }
}
    // 3. Fetch Questions based on Category/Round
    @GetMapping("/questions/{category}")
    public ResponseEntity<List<Question>> getStudentQuestions(@PathVariable String category) {
        try {
            QuizConfig config = configRepo.findById(1L).orElse(null);
            if (config == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

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

    // 4. 🔥 SMART SUBMISSION LOGIC (TOGGLE INTEGRATED) 🔥
  @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody Map<String, String> answers, HttpServletRequest request, Authentication authentication) {
        try {
            String mobile = authentication.getName(); 
            String name = getStudentNameFromToken(request); 
            
            QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
            List<Question> questions = questionService.getQuestionsByCategory(config.getActiveRound());

            // Correct evaluations scanning loop
            int score = (int) questions.stream().filter(q -> 
                q.getCorrectAns().trim().equalsIgnoreCase(answers.getOrDefault(String.valueOf(q.getId()), "").trim())
            ).count();

            // Object mapping
            Result r = new Result();
            r.setStudentMobile(mobile);
            r.setStudentName(name);
            r.setScore(score);
            
            // 🎯 FIXED: Sets max target question bounds dynamically from operational matrix
            r.setTotalQuestions(config.getQuestionLimit()); 
            
            r.setQuizLimit(config.getQuestionLimit());
            r.setQuizRound(config.getActiveRound());
            r.setAttempted(true);

            // Sibling Support Matching Strategy Injection
            java.util.Optional<User> studentProfileOpt = userRepo.findByMobileNumberAndName(mobile, name); 
            if (studentProfileOpt.isPresent()) {
                User studentProfile = studentProfileOpt.get();
                r.setSchoolName(studentProfile.getSchoolName()); 
                r.setArea(studentProfile.getArea());             
            }

            // Persistence routing handling smart toggles
            Map<String, Object> submissionResponse = resultService.saveOrUpdateResult(r);
            return ResponseEntity.ok(submissionResponse);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Internal Error: " + e.getMessage()));
        }
    }
    // 5. Active User Tracking
    @PostMapping("/quiz/enter")
    public ResponseEntity<?> enterQuiz() {
        AdminController.activeQuizUsers.incrementAndGet();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/quiz/exit")
    public ResponseEntity<?> exitQuiz() {
        if (AdminController.activeQuizUsers.get() > 0) {
            AdminController.activeQuizUsers.decrementAndGet();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/leaderboard/all")
    public ResponseEntity<?> getStudentLeaderboard() {
        // Sirf results data access ho raha hai, zero admin context
        return ResponseEntity.ok(resultRepo.findAllByOrderByScoreDesc());
    }
    

@GetMapping("/leaderboard/active")
public ResponseEntity<?> getActiveOnlyLeaderboard() {
    // 1. Live settings schema se active round uthao
    QuizConfig config = configRepo.findById(1L).orElse(null);
    if (config == null || config.getActiveRound() == null) {
        return ResponseEntity.ok(new ArrayList<>()); // Dynamic structural fallback
    }
    
    String liveRound = config.getActiveRound();
    System.out.println("Student request incoming - Restricting leaderboard to: " + liveRound);
    
    // 2. Pure results table mein se pichle quizes ka data ignore karke strictly current round filter karo
    List<Result> allRecords = resultRepo.findAll();
    List<Result> filteredResults = allRecords.stream()
            .filter(res -> res.getQuizRound() != null && res.getQuizRound().equalsIgnoreCase(liveRound))
            .sorted((r1, r2) -> Integer.compare(r2.getScore(), r1.getScore())) // Rank order listing
            .collect(Collectors.toList());
            
    return ResponseEntity.ok(filteredResults);
}
}