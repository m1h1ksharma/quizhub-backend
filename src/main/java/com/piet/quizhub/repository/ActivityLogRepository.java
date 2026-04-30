package com.piet.quizhub.repository;

import com.piet.quizhub.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    List<ActivityLog> findTop10ByOrderByTimestampDesc();
}