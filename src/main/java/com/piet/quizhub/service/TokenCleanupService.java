package com.piet.quizhub.service;

import com.piet.quizhub.repository.ResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class TokenCleanupService {

    @Autowired
    private ResetTokenRepository resetTokenRepo;

    // Har raat 12 baje chalega (Cron expression)
    // "0 0 0 * * ?" ka matlab: Second 0, Minute 0, Hour 0 (Midnight)
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void removeExpiredTokens() {
        System.out.println("CLEANUP JOB: Expired Tokens are being removed...");
        resetTokenRepo.deleteAllByExpiryDateBefore(LocalDateTime.now());
    }
}