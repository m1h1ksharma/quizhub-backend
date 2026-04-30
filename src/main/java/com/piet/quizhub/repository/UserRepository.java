package com.piet.quizhub.repository;

import com.piet.quizhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 1. Sibling Support: Ek hi mobile number par multiple profiles (e.g., brothers)
    List<User> findAllByMobileNumber(String mobileNumber);

    // 2. Student Login: Mobile Number + Name ka combination (Unique identity)
    Optional<User> findByMobileNumberAndName(String mobileNumber, String name);

    // 3. Admin Login: Email base lookup
    Optional<User> findByEmail(String email);

    // 4. Registration Check: Duplicate check for Name + Mobile
    boolean existsByMobileNumberAndName(String mobileNumber, String name);

    // 5. Admin Stats: Total Students ka count (Admins ko exclude karke)
    long countByRole(String role);

    /**
     * ⚠️ NOTE: 
     * findTopScore(), findAllByOrderByScoreDesc(), aur 
     * clearAllScores() yahan se hata diye gaye hain kyunki 
     * 'score' ab 'User' entity ka part nahi hai. 
     * Ye methods ab ResultRepository mein honge.
     */

    // Optional: Agar kabhi poore students ka data reset karna ho (Auth related)
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.role = 'STUDENT' WHERE u.role IS NULL")
    void initializeRoles();
}