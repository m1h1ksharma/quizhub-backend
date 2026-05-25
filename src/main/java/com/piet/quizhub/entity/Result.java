package com.piet.quizhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "results") 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) 
    private String studentMobile; 

    @Column(nullable = false)
    private String studentName;

    private int score;
    private int totalQuestions;
    private boolean attempted = false; 
    private String quizRound;  
    private int quizLimit;
    private String schoolName;
    private String area;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    @Version 
    private Integer version;
}