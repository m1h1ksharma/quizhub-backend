package com.piet.quizhub.service;

import com.piet.quizhub.entity.Result;
import com.piet.quizhub.entity.QuizSettings;
import com.piet.quizhub.repository.ResultRepository;
import com.piet.quizhub.repository.QuizSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ResultService {

    @Autowired private ResultRepository resultRepo;
    @Autowired private QuizSettingsRepository settingsRepo;

    @Transactional
    public Map<String, Object> saveOrUpdateResult(Result result) {
        // Industry Practice 1: Input Sanitization
        String name = result.getStudentName().trim();
        String mobile = result.getStudentMobile().trim();
        String round = result.getQuizRound().trim();

        // 1. Result Save/Update Logic
        Optional<Result> existing = resultRepo.findByStudentNameAndStudentMobileAndQuizRound(name, mobile, round);
        Result savedResult;

        if (existing.isPresent()) {
            savedResult = existing.get();
            savedResult.setScore(result.getScore());
            savedResult.setTotalQuestions(result.getTotalQuestions());
            savedResult.setAttempted(true);
            
            // ✅ FIX 1: Update block mein schoolName aur area inject karo taaki null overwrite ho sake!
            savedResult.setSchoolName(result.getSchoolName());
            savedResult.setArea(result.getArea());
            
            resultRepo.saveAndFlush(savedResult);
        } else {
            // ✅ FIX 2: Fresh result ke paas pehle se hi controller se schoolName aur area aa raha hai
            result.setId(null); 
            result.setAttempted(true);
            savedResult = resultRepo.saveAndFlush(result);
        }

        // 2. 🔥 SMART TOGGLE LOGIC 🔥
        Optional<QuizSettings> settingsOpt = settingsRepo.findById(round);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Quiz Submitted Successfully!");

        if (settingsOpt.isPresent() && settingsOpt.get().isShowResult()) {
            // ✅ CLASSROOM MODE: Marks bhej do
            response.put("showResult", true);
            response.put("score", savedResult.getScore());
            response.put("total", savedResult.getTotalQuestions());
        } else {
            // ❌ DRIVE MODE: Marks mat dikhao
            response.put("showResult", false);
        }

        return response;
    }
}