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

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "https://quizhub-frontend-six.vercel.app")
public class AdminController {

    @Autowired private QuestionRepository questionRepo;
    @Autowired private QuizConfigRepository configRepo;
    @Autowired private QuestionService questionService;
    @Autowired private UserRepository userRepository;
    @Autowired private ResultRepository resultRepo;

    // ==========================================
    // 1. DASHBOARD STATS
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
    // 2. EXCEL UPLOAD (SYNCED)
    // ==========================================
    @PostMapping("/questions/upload-excel")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file, @RequestParam("round") String round) {
        try {
            // excelToQuestions method from your ExcelHelper
            List<Question> list = ExcelHelper.excelToQuestions(file.getInputStream(), round);
            questionRepo.saveAll(list);
            questionService.refreshQuestions();
            return ResponseEntity.ok(Map.of("message", "Bulk Upload Successful!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 3. SETTINGS & TIMER
    // ==========================================
    @GetMapping("/settings/timer")
    public ResponseEntity<?> getTimer() {
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
            QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
            config.setId(1L);
            config.setTimerMinutes(Integer.parseInt(payload.get("timerMinutes").toString()));
            config.setQuestionLimit(Integer.parseInt(payload.get("questionLimit").toString()));
            config.setActiveRound(payload.getOrDefault("roundName", "Normal Quiz").toString());
            configRepo.save(config);
            return ResponseEntity.ok(Map.of("message", "Settings Updated Successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/questions/rounds")
    public ResponseEntity<List<String>> getUniqueRounds() {
        List<String> rounds = questionRepo.findAllUniqueCategories();
        if (rounds.isEmpty()) rounds = Arrays.asList("Normal Quiz");
        return ResponseEntity.ok(rounds);
    }

    // ==========================================
    // 4. QUESTION CRUD
    // ==========================================
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
        return ResponseEntity.ok(Map.of("message", "Question Saved!"));
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
            return ResponseEntity.ok(Map.of("message", "Question Updated!"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        questionRepo.deleteById(id);
        questionService.refreshQuestions();
        return ResponseEntity.ok(Map.of("message", "Question Deleted!"));
    }

    @DeleteMapping("/questions/clear-by-round")
    public ResponseEntity<?> clearByRound(@RequestParam String roundName) {
        questionRepo.deleteAll(questionRepo.findByCategory(roundName));
        questionService.refreshQuestions();
        return ResponseEntity.ok(Map.of("message", "Round Purged!"));
    }

    @DeleteMapping("/questions/delete-all")
    public ResponseEntity<?> deleteAllQuestions() {
        questionRepo.deleteAllInBatch();
        questionService.refreshQuestions();
        return ResponseEntity.ok(Map.of("message", "All Questions Deleted!"));
    }

    // ==========================================
    // 5. STUDENT RESULTS
    // ==========================================
    @GetMapping("/results/all")
    public ResponseEntity<?> getAllResults() {
        return ResponseEntity.ok(resultRepo.findAllByOrderByScoreDesc());
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<Result> getResultById(@PathVariable Long id) {
        return resultRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete-result/{id}")
    public ResponseEntity<?> deleteResult(@PathVariable Long id) {
        resultRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Result Deleted!"));
    }

    @DeleteMapping("/results/delete-all")
    public ResponseEntity<?> deleteAllResults() {
        resultRepo.deleteAllInBatch();
        return ResponseEntity.ok(Map.of("message", "All Results Wiped!"));
    }
}