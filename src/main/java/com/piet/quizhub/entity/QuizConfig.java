package com.piet.quizhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizConfig {

    @Id
    private Long id = 1L;

    private String activeRound = "Normal_Quiz"; 
    private int timerMinutes = 10;
    private String status = "ACTIVE"; 
    private int questionLimit = 50; 

    // Constructor fix
    public QuizConfig(Long id, String activeRound, int timerMinutes) {
        this.id = id;
        this.activeRound = activeRound;
        this.timerMinutes = timerMinutes;
    }
}