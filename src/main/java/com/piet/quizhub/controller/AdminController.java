package com.piet.quizhub.controller;

import com.piet.quizhub.entity.*;
import com.piet.quizhub.helper.ExcelHelper;
import com.piet.quizhub.repository.*;
import com.piet.quizhub.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "https://quizhub-frontend-six.vercel.app")
public class AdminController {

    @Autowired private QuestionRepository questionRepo;
    @Autowired private QuizConfigRepository configRepo;
    @Autowired private QuestionService questionService;
    @Autowired private AIService aiService;
    @Autowired private UserRepository userRepository;
    @Autowired private ResultRepository resultRepo;

    // ==========================================
    // 1. DASHBOARD & STATS[cite: 1]
    // ==========================================
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.countByRole("STUDENT"));
        stats.put("totalQuestions", questionRepo.count());
        stats.put("totalRounds", questionRepo.findAllUniqueCategories().size());

        Integer topScore = resultRepo.findTopScore();
        stats.put("topScore", topScore != null ? topScore : 0);
        stats.put("recentSubmissions", resultRepo.findTop10ByOrderByTimestampDesc());

        List<Map<String, Object>> graphData = new ArrayList<>();
        graphData.add(Map.of("range", "0-20%", "count", resultRepo.countByScoreBetween(0, 2)));
        graphData.add(Map.of("range", "21-40%", "count", resultRepo.countByScoreBetween(3, 4)));
        graphData.add(Map.of("range", "41-60%", "count", resultRepo.countByScoreBetween(5, 6)));
        graphData.add(Map.of("range", "61-80%", "count", resultRepo.countByScoreBetween(7, 8)));
        graphData.add(Map.of("range", "81-100%", "count", resultRepo.countByScoreBetween(9, 10)));
        stats.put("graphData", graphData);

        return ResponseEntity.ok(stats);
    }

    // ==========================================
    // 2. ROUNDS & SETTINGS
    // ==========================================
    @GetMapping("/questions/rounds")
    public ResponseEntity<List<String>> getUniqueRounds() {
        List<String> rounds = questionRepo.findAllUniqueCategories();
        if (rounds.isEmpty()) rounds = Arrays.asList("Normal Quiz");
        return ResponseEntity.ok(rounds);
    }

    @GetMapping("/settings/timer")
    public ResponseEntity<?> getTimer() {
        // Fetch config with ID 1 or create default to avoid errors[cite: 1]
        QuizConfig config = configRepo.findById(1L).orElseGet(() -> {
            QuizConfig c = new QuizConfig();
            c.setId(1L);
            c.setActiveRound("Normal Quiz");
            c.setTimerMinutes(10);
            c.setQuestionLimit(50);
            return c;
        });
        return ResponseEntity.ok(config);
    }

    @PostMapping("/settings/update-timer")
    public ResponseEntity<?> updateRoundTimer(@RequestBody Map<String, Object> payload) {
        try {
            // Frontend keys match: timerMinutes, questionLimit, roundName[cite: 3]
            int timer = Integer.parseInt(payload.get("timerMinutes").toString());
            int limit = Integer.parseInt(payload.get("questionLimit").toString());
            String round = payload.getOrDefault("roundName", "Normal Quiz").toString();

            QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
            config.setId(1L);
            config.setTimerMinutes(timer);
            config.setQuestionLimit(limit);
            config.setActiveRound(round);
            configRepo.save(config);

            return ResponseEntity.ok(Map.of("message", "Settings Updated Successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 3. QUESTION CRUD[cite: 4]
    // ==========================================
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

    @DeleteMapping("/questions/round/{roundName}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteRound(@PathVariable String roundName) {
        try {
            List<Question> questions = questionRepo.findByCategory(roundName);
            questionRepo.deleteAll(questions);
            questionService.refreshQuestions();
            return ResponseEntity.ok(Map.of("message", roundName + " round deleted!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Delete failed"));
        }
    }

    @DeleteMapping("/questions/clear-by-round")
    public ResponseEntity<?> clearQuestionsByRound(@RequestParam String roundName) {
        try {
            List<Question> questions = questionRepo.findByCategory(roundName);
            questionRepo.deleteAll(questions);
            return ResponseEntity.ok("Questions cleared successfully for " + roundName);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    // ==========================================
    // 4. RESULTS MANAGEMENT[cite: 5]
    // ==========================================
    @GetMapping("/results/all")
    public ResponseEntity<?> getAllResults() {
        return ResponseEntity.ok(resultRepo.findAllByOrderByScoreDesc());
    }

    @DeleteMapping("/delete-result/{id}")
    public ResponseEntity<?> deleteResult(@PathVariable Long id) {
        if (!resultRepo.existsById(id)) return ResponseEntity.notFound().build();
        resultRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Result deleted!"));
    }

    @DeleteMapping("/results/delete-all")
    public ResponseEntity<?> deleteAllResults() {
        resultRepo.deleteAllInBatch();
        return ResponseEntity.ok(Map.of("message", "All results cleared!"));
    }
}