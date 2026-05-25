package com.piet.quizhub.controller;

import com.piet.quizhub.entity.*;
import com.piet.quizhub.helper.ExcelHelper;
import com.piet.quizhub.repository.*;
import com.piet.quizhub.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "https://quizhub-frontend-six.vercel.app")
// @CrossOrigin(origins = "http://localhost:3000")

public class AdminController {

    @Autowired
    private QuestionRepository questionRepo;
    @Autowired
    private QuizConfigRepository configRepo;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private AIService aiService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ResultRepository resultRepo;
    @Autowired
    private QuizSettingsRepository quizSettingsRepository;

    // static counter
    public static final AtomicInteger activeQuizUsers = new AtomicInteger(0);

   // --- DASHBOARD & STATS (FIXED) ---
@GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Basic Info
        stats.put("activeStudents", activeQuizUsers.get()); 
        stats.put("totalUsers", userRepository.countByRole("STUDENT"));
        stats.put("totalQuestions", questionRepo.count());
        
        List<String> rounds = questionRepo.findAllUniqueCategories();
        stats.put("totalRounds", rounds != null ? rounds.size() : 0);
        
        Integer topScore = resultRepo.findTopScore();
        stats.put("topScore", topScore != null ? topScore : 0);
        stats.put("recentSubmissions", resultRepo.findTop10ByOrderByTimestampDesc());

        // ✅ DYNAMIC GRAPH LOGIC (0-100 Marks Handling)
        List<Map<String, Object>> graphData = new ArrayList<>();
        long totalQ = questionRepo.count(); 
        if(totalQ <= 0) totalQ = 10; // Fallback

        // Hum 5 Buckets banate hain dynamic labels ke saath
        int step = (int) Math.ceil((double) totalQ / 5);
        for (int i = 0; i < 5; i++) {
            int start = i * step;
            int end = (i == 4) ? (int) totalQ : (start + step - 1);
            
            long count = resultRepo.countByScoreBetween(start, end);
            
            Map<String, Object> point = new HashMap<>();
            point.put("range", start + "-" + end); 
            point.put("count", count);
            graphData.add(point);
        }
        stats.put("graphData", graphData);

        return ResponseEntity.ok()
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .body(stats);
    }
    // --- LEADERBOARD & RESULTS ---
    @GetMapping("/results/all")
    public ResponseEntity<?> getAllResults() {
        return ResponseEntity.ok(resultRepo.findAllByOrderByScoreDesc());
    }

    @GetMapping("/results/filter/global-toppers")
public ResponseEntity<?> getGlobalToppers() {
    return ResponseEntity.ok(resultRepo.findAllByOrderByScoreDesc());
}

@GetMapping("/results/filter/by-area")
public ResponseEntity<?> getResultsByArea(@RequestParam String area) {
    return ResponseEntity.ok(resultRepo.findByAreaOrderByScoreDesc(area));
}

@GetMapping("/results/filter/by-school")
public ResponseEntity<?> getResultsBySchool(@RequestParam String schoolName) {
    return ResponseEntity.ok(resultRepo.findBySchoolNameOrderByScoreDesc(schoolName));
}

@GetMapping("/results/filter/top-two-per-school")
public ResponseEntity<?> getTopTwoPerSchool(@RequestParam(defaultValue = "2") int limit) {
    return ResponseEntity.ok(resultRepo.findTopXStudentsPerSchool(limit));
}

    @GetMapping("/results/{id}")
    public ResponseEntity<Result> getResultById(@PathVariable Long id) {
        return resultRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

  @PutMapping("/update-result/{id}")
public ResponseEntity<?> updateStudentResult(@PathVariable Long id, @RequestBody Result updatedResult) {
    return resultRepo.findById(id).map(res -> {
        // 1. Purani details aur naye mobile data ko tracking parameters mein lo
        String oldMobile = res.getStudentMobile();
        String oldName = res.getStudentName();
        
        // 2. Leaderboard Result Matrix Table Update
        res.setStudentName(updatedResult.getStudentName());
        res.setStudentMobile(updatedResult.getStudentMobile());
        res.setScore(updatedResult.getScore());
        res.setQuizRound(updatedResult.getQuizRound());
        res.setSchoolName(updatedResult.getSchoolName());
        res.setArea(updatedResult.getArea());
        resultRepo.save(res);
        
        // 3. 🎯 JADU HERS: Sibling support repo rule ke threw bache ki User table profile bhi auto-correct karo!
        java.util.Optional<User> userProfileOpt = userRepository.findByMobileNumberAndName(oldMobile, oldName);
        if (userProfileOpt.isPresent()) {
            User userProfile = userProfileOpt.get();
            userProfile.setName(updatedResult.getStudentName());
            userProfile.setMobileNumber(updatedResult.getStudentMobile());
            userProfile.setSchoolName(updatedResult.getSchoolName());
            userProfile.setArea(updatedResult.getArea());
            userRepository.save(userProfile); // User registry entry perfectly synced!
        }
        
        return ResponseEntity.ok(Map.of("message", "Result and Student Profile synced & corrected successfully!"));
    }).orElse(ResponseEntity.status(404).body(Map.of("message", "Result not found!")));
}

    @DeleteMapping("/delete-result/{id}")
    public ResponseEntity<?> deleteResult(@PathVariable Long id) {
        resultRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Result deleted!"));
    }

    @DeleteMapping("/results/delete-all")
    public ResponseEntity<?> deleteAllResults() {
        resultRepo.deleteAllInBatch();
        return ResponseEntity.ok(Map.of("message", "All results cleared!"));
    }

    // --- ROUNDS & SETTINGS ---
    @GetMapping("/questions/rounds")
    public ResponseEntity<List<String>> getUniqueRounds() {
        List<String> rounds = questionRepo.findAllUniqueCategories();
        if (rounds.isEmpty())
            rounds = Arrays.asList("Normal Quiz");
        return ResponseEntity.ok(rounds);
    }

    // --- SETTINGS ---
    @GetMapping("/settings/timer")
    public ResponseEntity<?> getTimer() {
        QuizConfig config = configRepo.findById(1L).orElseGet(() -> {
            QuizConfig newConfig = new QuizConfig();
            newConfig.setId(1L);
            newConfig.setActiveRound("Normal Quiz");
            newConfig.setTimerMinutes(10);
            newConfig.setQuestionLimit(50);
            return newConfig;
        });
        return ResponseEntity.ok(config);
    }

    @PostMapping("/settings/update-timer")
    public ResponseEntity<?> updateRoundTimer(@RequestBody Map<String, Object> payload) {
        try {
            QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
            config.setId(1L);
            config.setTimerMinutes(Integer.parseInt(payload.get("timerMinutes").toString()));
            config.setQuestionLimit(Integer.parseInt(payload.get("questionLimit").toString()));
            config.setActiveRound(payload.getOrDefault("roundName", "Normal Quiz").toString());
            configRepo.save(config);
            return ResponseEntity.ok(Map.of("message", "Settings Updated!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // --- QUESTION CRUD ---
    @GetMapping("/questions")
    public List<Question> getAllQuestions() {
        return questionRepo.findAll();
    }

    @PostMapping("/questions/add")
    public ResponseEntity<?> addQuestion(@RequestBody Question question) {
        questionRepo.save(question);
        questionService.refreshQuestions();
        return ResponseEntity.ok(Map.of("message", "Saved!"));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id, @RequestBody Question updatedQ) {
        return questionRepo.findById(id).map(q -> {
            q.setContent(updatedQ.getContent());
            q.setOptionA(updatedQ.getOptionA());
            q.setOptionB(updatedQ.getOptionB());
            q.setOptionC(updatedQ.getOptionC());
            q.setOptionD(updatedQ.getOptionD());
            q.setCorrectAns(updatedQ.getCorrectAns());
            q.setCategory(updatedQ.getCategory());
            questionRepo.save(q);
            return ResponseEntity.ok(Map.of("message", "Updated!"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        questionRepo.deleteById(id);
        questionService.refreshQuestions();
        return ResponseEntity.ok(Map.of("message", "Deleted!"));
    }

    // Is mapping ko update karo taaki ye frontend ke path se match kare
    @DeleteMapping("/questions/round/{roundName}")
    public ResponseEntity<?> clearByRound(@PathVariable String roundName) {
        // URL encoded space handle karne ke liye
        String cleanRound = roundName.replace("%20", " ").trim();

        System.out.println("Deleting questions for round: " + cleanRound);

        questionService.deleteQuestionsByCategory(cleanRound);
        questionService.refreshQuestions();

        return ResponseEntity.ok(Map.of("message", "Round '" + cleanRound + "' data cleared!"));
    }

    @DeleteMapping("/questions/delete-all")
    public ResponseEntity<?> deleteAllQuestions() {
        questionRepo.deleteAllInBatch();
        questionService.refreshQuestions();
        return ResponseEntity.ok(Map.of("message", "Bank Cleared!"));
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        return questionRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- EXCEL & AI ---
    @PostMapping("/questions/upload-excel")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file,
            @RequestParam("round") String round) {
        try {
            List<Question> questions = ExcelHelper.excelToQuestions(file.getInputStream(), round);
            questionRepo.saveAll(questions);
            questionService.refreshQuestions();
            return ResponseEntity.ok(Map.of("message", "Uploaded " + questions.size() + " questions!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Excel Error: " + e.getMessage());
        }
    }

    @PostMapping("/questions/ai-generate")
    public ResponseEntity<?> generateAIQuestions(@RequestBody Map<String, Object> request) {
        try {
            String topic = request.get("topic").toString();
            int count = Integer.parseInt(request.get("count").toString());
            String jsonResponse = aiService.getAIQuestions(topic, count, "Medium");
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("AI Error: " + e.getMessage());
        }
    }

    @PostMapping("/questions/ai-save-bulk")
    public ResponseEntity<?> saveAIQuestions(@RequestBody List<Question> questions) {
        questionRepo.saveAll(questions);
        questionService.refreshQuestions();
        return ResponseEntity.ok(Map.of("message", "AI Questions saved!"));
    }

    @PutMapping("/settings/toggle-result/{roundName}")
    public ResponseEntity<?> toggleResult(@PathVariable String roundName, @RequestParam boolean status) {
        try {
            String cleanRound = roundName.replace("%20", " ").trim();
            QuizSettings settings = quizSettingsRepository.findById(cleanRound)
                    .orElse(new QuizSettings(cleanRound, 10, false, false));

            settings.setShowResult(status);
            quizSettingsRepository.save(settings);

            return ResponseEntity.ok(Map.of(
                    "status", true,
                    "message", "Result visibility updated to " + status + " for " + cleanRound));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/settings/{roundName}")
    public ResponseEntity<?> getSettings(@PathVariable String roundName) {
        return quizSettingsRepository.findById(roundName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new QuizSettings(roundName, 10, false, false)));
    }

}