package com.piet.quizhub.repository;

import com.piet.quizhub.entity.QuizConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizConfigRepository extends JpaRepository<QuizConfig, Long> {
}
