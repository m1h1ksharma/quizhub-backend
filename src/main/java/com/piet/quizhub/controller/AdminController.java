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

    // ================= 📊 DASHBOARD & STATS =================

    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Core Counts
        stats.put("totalUsers", userRepository.countByRole("STUDENT"));
        stats.put("totalQuestions", questionRepo.count());
        stats.put("totalRounds", questionRepo.findAllUniqueCategories().size());
        
        // 2. High Score
        Integer topScore = resultRepo.findTopScore();
        stats.put("topScore", topScore != null ? topScore : 0);

        // 3. 🔥 Live Feed (Recent 10 Submissions)
        stats.put("recentSubmissions", resultRepo.findTop10ByOrderByTimestampDesc());

        // 4. 📈 Graph Data: Score Distribution (Real Logic)
        
        List<Map<String, Object>> graphData = new ArrayList<>();
        graphData.add(Map.of("range", "0-20%", "count", resultRepo.countByScoreBetween(0, 2)));
        graphData.add(Map.of("range", "21-40%", "count", resultRepo.countByScoreBetween(3, 4)));
        graphData.add(Map.of("range", "41-60%", "count", resultRepo.countByScoreBetween(5, 6)));
        graphData.add(Map.of("range", "61-80%", "count", resultRepo.countByScoreBetween(7, 8)));
        graphData.add(Map.of("range", "81-100%", "count", resultRepo.countByScoreBetween(9, 10)));
        
        stats.put("graphData", graphData);

        return ResponseEntity.ok(stats);
    }

    // ================= 🏆 LEADERBOARD & RESULTS =================

    @GetMapping("/results/all")
    public ResponseEntity<?> getAllResults() {
        return ResponseEntity.ok(resultRepo.findAllByOrderByScoreDesc());
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<Result> getResultById(@PathVariable Long id) {
        return resultRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/update-result/{id}")
    public ResponseEntity<?> updateStudentResult(@PathVariable Long id, @RequestBody Result updatedResult) {
        return resultRepo.findById(id).map(res -> {
            res.setStudentName(updatedResult.getStudentName());
            res.setStudentMobile(updatedResult.getStudentMobile());
            res.setScore(updatedResult.getScore());
            res.setTotalQuestions(updatedResult.getTotalQuestions());
            res.setQuizRound(updatedResult.getQuizRound());
            resultRepo.save(res);
            return ResponseEntity.ok(Map.of("message", "Result updated successfully! ✅"));
        }).orElse(ResponseEntity.status(404).body(Map.of("message", "Result not found!")));
    }

    @DeleteMapping("/delete-result/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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

    // ================= ⚙️ ROUNDS & SETTINGS =================

    @GetMapping("/questions/rounds")
    public ResponseEntity<List<String>> getUniqueRounds() {
        List<String> rounds = questionRepo.findAllUniqueCategories();
        if (rounds.isEmpty()) rounds = Arrays.asList("Normal_Quiz");
        return ResponseEntity.ok(rounds);
    }

    @GetMapping("/settings/timer")
    public ResponseEntity<?> getTimer() {
        QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig(1L, "Normal_Quiz", 10));
        return ResponseEntity.ok(config);
    }

@PostMapping("/settings/update-timer")
public ResponseEntity<?> updateRoundTimer(@RequestBody Map<String, Object> payload) {
    try {
        int timer = Integer.parseInt(payload.get("timerMinutes").toString());
        int limit = Integer.parseInt(payload.get("questionLimit").toString()); 
        String round = payload.getOrDefault("roundName", "Normal_Quiz").toString();

        QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
        config.setId(1L);
        config.setTimerMinutes(timer);
        config.setQuestionLimit(limit);
        
        
        config.setActiveRound(round); 
        
        configRepo.save(config);
        System.out.println("✅ DB Updated: " + round);
        return ResponseEntity.ok(Map.of("message", "Settings Updated!"));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

    @PostMapping("/settings/update-status")
    public ResponseEntity<?> updateQuizStatus(@RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
        config.setStatus(status);
        configRepo.save(config);
        return ResponseEntity.ok(Map.of("message", "Quiz status changed to " + status));
    }

    // ================= 📝 QUESTION CRUD =================

    @GetMapping("/questions")
    public List<Question> getAllQuestions() {
        return questionRepo.findAll();
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        return questionRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
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

    @PostMapping("/questions/upload-excel")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file, @RequestParam("round") String round) {
        try {
            List<Question> questions = ExcelHelper.excelToQuestions(file.getInputStream(), round);
            questionRepo.saveAll(questions);
            questionService.refreshQuestions();
            return ResponseEntity.ok(Map.of("message", "Uploaded " + questions.size() + " questions!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Excel Error: " + e.getMessage());
        }
    }

    // ================= 🤖 AI GENERATOR =================

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

    @DeleteMapping("/questions/round/{roundName}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> deleteRound(@PathVariable String roundName) {
    try {
        // 1. Us category ke saare questions delete karo
        List<Question> questions = questionRepo.findByCategory(roundName);
        questionRepo.deleteAll(questions);
        
        // 2. Cache refresh karo (agar use kar rahe ho)
        questionService.refreshQuestions();
        
        return ResponseEntity.ok(Map.of("message", roundName + " round deleted successfully!"));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", "Delete failed"));
    }
}
// AdminController.java

@DeleteMapping("/questions/clear-by-round")
public ResponseEntity<?> clearQuestionsByRound(@RequestParam String roundName) {
    try {
        // Service ko bolna ki sirf is round ke questions udaaye
        questionService.deleteQuestionsByCategory(roundName);
        return ResponseEntity.ok("Questions cleared successfully for round: " + roundName);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Error clearing questions: " + e.getMessage());
    }
}
}