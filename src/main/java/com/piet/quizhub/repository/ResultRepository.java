package com.piet.quizhub.repository;

import com.piet.quizhub.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    /**
     * 1. UNIQUE CHECK & SIBLING LOGIC
     * Ye method ensure karta hai ki agar same mobile se alag naam ka bacha (sibling) 
     * quiz de raha hai, toh naya record bane.
     */
    Optional<Result> findByStudentNameAndStudentMobileAndQuizRound(String studentName, String studentMobile, String quizRound);

    /**
     * 2. LEADERBOARD
     * Saare results score ke hisaab se descending order mein.
     */
    List<Result> findAllByOrderByScoreDesc();

    /**
     * 3. TOP SCORE
     * Dashboard ke "Highest Score" card ke liye.
     */
    @Query("SELECT MAX(r.score) FROM Result r")
    Integer findTopScore();

    /**
     * 4. LIVE ACTIVITY FEED
     * Dashboard par latest 10 submissions dikhane ke liye.
     * Ensure karo ki Result entity mein 'timestamp' field @CreationTimestamp ke saath ho.
     */
    List<Result> findTop10ByOrderByTimestampDesc();

    /**
     * 5. GRAPH LOGIC
     * Score distribution ranges count karne ke liye (e.g., 0-2, 3-4 score kitne bacho ke hain).
     */
    long countByScoreBetween(int start, int end);

    /**
     * 6. CLEANUP (Admin Action)
     * Saare results delete karne ke liye (Delete All button).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Result")
    void deleteAllResults();

    /**
     * 7. ROUND STATS
     * Ek specific round mein total kitne bacho ne attempt kiya.
     */
    long countByQuizRound(String quizRound);

    /**
     * 8. STUDENT HISTORY
     * Kisi specific student ke saare rounds ka data check karne ke liye.
     */
    Optional<Result> findByStudentMobileAndStudentName(String studentMobile, String studentName);
}