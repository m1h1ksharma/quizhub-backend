package com.piet.quizhub.controller;

import com.piet.quizhub.entity.*;
import com.piet.quizhub.helper.ExcelHelper;
import com.piet.quizhub.repository.*;
import com.piet.quizhub.service.*;
import org.springframework.beans.factory.annotation.Autowired;
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

    // --- Dashboard Stats ---
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.countByRole("STUDENT"));
        stats.put("totalQuestions", questionRepo.count());
        stats.put("totalRounds", questionRepo.findAllUniqueCategories().size());
        
        Integer topScore = resultRepo.findTopScore();
        stats.put("topScore", topScore != null ? topScore : 0);
        stats.put("recentSubmissions", resultRepo.findTop10ByOrderByTimestampDesc());
        return ResponseEntity.ok(stats);
    }

    // --- Excel Upload (Synced with your ExcelHelper) ---
    @PostMapping("/questions/upload-excel")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file, @RequestParam("round") String round) {
        try {
            // Method name synced: excelToQuestions
            List<Question> list = ExcelHelper.excelToQuestions(file.getInputStream(), round);
            questionRepo.saveAll(list);
            questionService.refreshQuestions();
            return ResponseEntity.ok(Map.of("message", "Bulk Upload Successful!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // --- Quiz Settings & Timer ---
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
        QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig());
        config.setId(1L);
        config.setTimerMinutes(Integer.parseInt(payload.get("timerMinutes").toString()));
        config.setQuestionLimit(Integer.parseInt(payload.get("questionLimit").toString()));
        config.setActiveRound(payload.get("roundName").toString());
        configRepo.save(config);
        return ResponseEntity.ok(Map.of("message", "Updated Successfully!"));
    }

    // --- Question & Results Management ---
    @GetMapping("/questions")
    public List<Question> getAllQuestions() { return questionRepo.findAll(); }

    @GetMapping("/questions/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        return questionRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/questions/delete-all")
    public ResponseEntity<?> deleteAllQuestions() {
        questionRepo.deleteAllInBatch();
        questionService.refreshQuestions();
        return ResponseEntity.ok(Map.of("message", "All Questions Deleted!"));
    }

    @GetMapping("/results/all")
    public ResponseEntity<?> getAllResults() { return ResponseEntity.ok(resultRepo.findAllByOrderByScoreDesc()); }

    @DeleteMapping("/results/delete-all")
    public ResponseEntity<?> deleteAllResults() {
        resultRepo.deleteAllInBatch();
        return ResponseEntity.ok(Map.of("message", "Database Cleared!"));
    }
}