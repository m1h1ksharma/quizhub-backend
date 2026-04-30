package com.piet.quizhub.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "questions")
@Data
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT") // Bada question handle karne ke liye
    private String content;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
//    @JsonIgnore
    private String correctAns; // "A", "B", "C", ya "D"
    // Question entity mein ye field add karo
private String category; // Example: "Round 1", "Java Basics", "Aptitude"
}