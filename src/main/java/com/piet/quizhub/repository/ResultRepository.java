package com.piet.quizhub.repository;

import com.piet.quizhub.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    /** 1. UNIQUE CHECK & SIBLING LOGIC*/
    Optional<Result> findByStudentNameAndStudentMobileAndQuizRound(String studentName, String studentMobile, String quizRound);

    /** * 2. LEADERBOARD */
    List<Result> findAllByOrderByScoreDesc();

    // 3. Dynamic Area Filter
    List<Result> findByAreaOrderByScoreDesc(String area);

    /*** 4. TOP SCORE*/
    @Query("SELECT MAX(r.score) FROM Result r")
    Integer findTopScore();

    /*** 5. LIVE ACTIVITY FEED*/
    List<Result> findTop10ByOrderByTimestampDesc();

    /**
     * 6. GRAPH LOGIC*/
    long countByScoreBetween(int start, int end);

    /*** 7. CLEANUP (Admin Action) */
    @Modifying
    @Transactional
    @Query("DELETE FROM Result")
    void deleteAllResults();

    /*** 8. ROUND STATS */
    long countByQuizRound(String quizRound);

    /* 9. STUDENT HISTORY */
    Optional<Result> findByStudentMobileAndStudentName(String studentMobile, String studentName);

    // 10. School/College Wise Filter
    List<Result> findBySchoolNameOrderByScoreDesc(String schoolName);

    // 4. THE MASTER FILTER
    @Query(value = "SELECT * FROM (" +
                   "  SELECT r.*, ROW_NUMBER() OVER (PARTITION BY r.school_name ORDER BY r.score DESC) as row_num " +
                   "  FROM results r" +
                   ") ranked WHERE ranked.row_num <= :limit", nativeQuery = true)
    List<Result> findTopXStudentsPerSchool(@Param("limit") int limit);
}