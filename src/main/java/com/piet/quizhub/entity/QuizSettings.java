package com.piet.quizhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Getter
@Setter
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSettings {

    @Id
    private String roundName; 
    private int timerMinutes; 
    private boolean isLive;   
    
    @Column(name = "show_result")
    private boolean showResult = false;
   
}