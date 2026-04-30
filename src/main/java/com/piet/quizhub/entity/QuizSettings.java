package com.piet.quizhub.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSettings {

    @Id
    private String roundName; 
    private int timerMinutes; 
    private boolean isLive;   
}