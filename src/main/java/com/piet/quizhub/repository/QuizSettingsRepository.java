package com.piet.quizhub.repository;

import com.piet.quizhub.entity.QuizSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import jakarta.transaction.Transactional;
import java.util.Optional;

public interface QuizSettingsRepository extends JpaRepository<QuizSettings, String> {

    // Sirf ek round live ho sakta hai, baaki sabko false karne ke liye
    @Transactional
    @Modifying
    @Query("UPDATE QuizSettings s SET s.isLive = false")
    void disableAllRounds();

    Optional<QuizSettings> findByIsLiveTrue();
    
}