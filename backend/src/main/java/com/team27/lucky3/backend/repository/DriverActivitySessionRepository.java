package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.DriverActivitySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverActivitySessionRepository extends JpaRepository<DriverActivitySession, Long> {
    
    /**
     * Find the current active session (no endTime) for a driver
     */
    Optional<DriverActivitySession> findByDriverIdAndEndTimeIsNull(Long driverId);
    
    /**
     * Find all sessions for a driver that started or ended within a time range
     */
    @Query("SELECT s FROM DriverActivitySession s WHERE s.driver.id = :driverId " +
           "AND (s.startTime >= :since OR (s.endTime IS NULL OR s.endTime >= :since))")
    List<DriverActivitySession> findSessionsSince(@Param("driverId") Long driverId, @Param("since") LocalDateTime since);
}
