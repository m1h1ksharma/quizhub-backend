package com.piet.quizhub.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.piet.quizhub.entity.PasswordResetToken;

public interface ResetTokenRepository extends JpaRepository<PasswordResetToken, Long> { Optional<PasswordResetToken> findByToken(String token); 
    void deleteAllByExpiryDateBefore(LocalDateTime now);
}
