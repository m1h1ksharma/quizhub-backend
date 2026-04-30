package com.piet.quizhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
    // 🛡️ Student: Name + Mobile unique rahega
    @UniqueConstraint(columnNames = {"name", "mobileNumber"}),
    // 🛡️ Admin: Email unique rahega
    @UniqueConstraint(columnNames = {"email"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- BASIC INFO ---
    @Column(nullable = false)
    private String name;
    
    private String fatherName;

    @Column(nullable = false)
    private String mobileNumber; 

    private String email; 

    // --- ACADEMIC & LOCATION INFO ---
    private String schoolName;
    private String city;
    private String area;
    private String classLevel; 
    private String stream;
    
    // --- AUTH & ROLES ---
    @Column(nullable = false)
    private String password;

    private String role = "STUDENT"; // "STUDENT" or "ADMIN"

    /** * ⚠️ NOTE: Score, Attempted, aur QuizRound yahan se hata diye hain.
     * Wo sab ab 'Result' entity mein store honge.
     */

    // ================= SECURITY METHODS =================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
    }

    @Override
    public String getUsername() {
        // Admin logins via Email, Student via Mobile
        if ("ADMIN".equalsIgnoreCase(this.role)) {
            return (email != null) ? email : mobileNumber;
        }
        return this.mobileNumber;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}