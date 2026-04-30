package com.piet.quizhub.controller;

import com.piet.quizhub.entity.Question;
import com.piet.quizhub.repository.QuestionRepository;
import com.piet.quizhub.repository.QuizConfigRepository;
import com.piet.quizhub.entity.QuizConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/questions")
@CrossOrigin(origins = "https://quizhub-frontend-six.vercel.app")
public class QuestionController {

    @Autowired
    private QuestionRepository questionRepo;

    @Autowired
    private QuizConfigRepository configRepo;

    // 🔥 FIX: getRoundName() ko hata kar getActiveRound() use kiya
    @GetMapping("/playquiz")
    public ResponseEntity<?> getQuizQuestions() {
        // Step 1: Admin ki set ki hui config uthao
        QuizConfig config = configRepo.findById(1L).orElse(null);
        
        // --- FIX: Yahan 'activeRound' use karo ---
        if (config == null || config.getActiveRound() == null) {
            return ResponseEntity.badRequest().body("No active quiz found in system!");
        }

        String targetRound = config.getActiveRound();
        System.out.println("Fetching questions for active round: " + targetRound);

        // Step 2: Questions filter karo (Case Insensitive)
        List<Question> allQs = questionRepo.findAll();
        List<Question> roundQs = allQs.stream()
                .filter(q -> q.getCategory() != null && q.getCategory().equalsIgnoreCase(targetRound))
                .collect(Collectors.toList());

        if (roundQs.isEmpty()) {
            return ResponseEntity.status(404).body("No questions found for round: " + targetRound);
        }

        // Step 3: Shuffle
        Collections.shuffle(roundQs);

        // Step 4: Admin limit apply karo
        int limit = Math.min(roundQs.size(), config.getQuestionLimit());
        return ResponseEntity.ok(roundQs.subList(0, limit));
    }

    // 2. Timer configuration return karega
    @GetMapping("/quiz-timer")
    public ResponseEntity<?> getQuizTimer() {
        // Humesha ID 1L se fetch karo taaki refresh par wahi data mile
        QuizConfig config = configRepo.findById(1L).orElse(new QuizConfig(1L, "Normal_Quiz", 10));
        return ResponseEntity.ok(config);
    }

    @GetMapping("/all")
    public List<Question> getAllQuestions() {
        return questionRepo.findAll();
    }
}