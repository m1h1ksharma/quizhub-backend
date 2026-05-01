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

    // --- DASHBOARD & STATS ---
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

    // --- LEADERBOARD & RESULTS ---
    @GetMapping("/results/all")
    public ResponseEntity<?> getAllResults() {
        return ResponseEntity.ok(resultRepo.findAllByOrderByScoreDesc());
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<Result> getResultById(@PathVariable Long id) {
        return resultRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete-result/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteResult(@PathVariable Long id) {
        resultRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Result deleted!"));
    }

    @DeleteMapping("/results/delete-all")
    public ResponseEntity<?> deleteAllResults() {
        resultRepo.deleteAllInBatch();
        return ResponseEntity.ok(Map.of("message", "All results cleared!"));
    }

    // --- SETTINGS ---
    @GetMapping("/settings/timer")
    public ResponseEntity<?> getTimer() {
        QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig(1L, "Normal Quiz", 10));
        return ResponseEntity.ok(config);
    }

    @PostMapping("/settings/update-timer")
    public ResponseEntity<?> updateRoundTimer(@RequestBody Map<String, Object> payload) {
        try {
            int timer = Integer.parseInt(payload.get("timerMinutes").toString());
            int limit = Integer.parseInt(payload.get("questionLimit").toString());
            String round = payload.getOrDefault("roundName", "Normal Quiz").toString();

            QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
            config.setId(1L);
            config.setTimerMinutes(timer);
            config.setQuestionLimit(limit);
            config.setActiveRound(round);
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

    // --- AI GENERATOR ---
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

    @DeleteMapping("/questions/clear-by-round")
    public ResponseEntity<?> clearQuestionsByRound(@RequestParam String roundName) {
        try {
            questionService.deleteQuestionsByCategory(roundName);
            return ResponseEntity.ok("Questions cleared successfully for " + roundName);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}