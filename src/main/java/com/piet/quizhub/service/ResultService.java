package com.piet.quizhub.service;

import com.piet.quizhub.entity.Result;
import com.piet.quizhub.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class ResultService {

    @Autowired
    private ResultRepository resultRepo;

    @Transactional
    public synchronized void saveOrUpdateResult(Result result) {
        // Industry Practice 1: Input Sanitization
        String name = result.getStudentName().trim();
        String mobile = result.getStudentMobile().trim();
        String round = result.getQuizRound().trim();

        // Industry Practice 2: Check for existing attempt
        Optional<Result> existing = resultRepo.findByStudentNameAndStudentMobileAndQuizRound(name, mobile, round);

        if (existing.isPresent()) {
            // Update mode: Wahi student hai
            Result res = existing.get();
            res.setScore(result.getScore());
            res.setTotalQuestions(result.getTotalQuestions());
            res.setAttempted(true);
            resultRepo.saveAndFlush(res); 
        } else {
            // New mode: Alag student ya sibling
            // 🔥 Industry Practice 3: Clear ID to ensure INSERT and use Flush
            result.setId(null); 
            result.setAttempted(true);
            resultRepo.saveAndFlush(result); 
        }
    }
}