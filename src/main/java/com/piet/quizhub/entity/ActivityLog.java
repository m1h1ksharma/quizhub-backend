package com.piet.quizhub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String userName;
    private String action; // REGISTER, LOGIN, SUBMIT
    private String details; // e.g., "Score: 85"
    private LocalDateTime timestamp;

    public ActivityLog(String userName, String action, String details) {
        this.userName = userName;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
}