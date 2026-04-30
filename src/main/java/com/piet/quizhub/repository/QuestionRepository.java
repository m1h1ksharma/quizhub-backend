package com.piet.quizhub.repository;

import com.piet.quizhub.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // ✅ Import for delete/update
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional; // ✅ Import for transactions
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // Unique rounds fetch karne ki query
    @Query("SELECT DISTINCT q.category FROM Question q WHERE q.category IS NOT NULL")
    List<String> findAllUniqueCategories();
    
    // Kisi specific round ke questions nikalne ke liye
    List<Question> findByCategory(String category);

    long countByCategory(String category);

    // 🔥 NEW: Delete all questions of a specific category
    @Modifying
    @Transactional
    @Query("DELETE FROM Question q WHERE q.category = :category")
    void deleteByCategory(@Param("category") String category);
}