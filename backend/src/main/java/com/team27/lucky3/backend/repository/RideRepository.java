package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
//     @Query("SELECT r FROM Ride r WHERE " +
//            "(:driverId IS NULL OR r.driver.id = :driverId) AND " +
//            "(:status IS NULL OR r.status = :status) AND " +
//            "(:passengerId IS NULL OR :passengerId IN (SELECT p.id FROM r.passengers p)) AND " +
//            "(:fromDate IS NULL OR r.startTime >= :fromDate) AND " +
//            "(:toDate IS NULL OR r.endTime <= :toDate)")
//     Page<Ride> findAllByFilters(
//             @Param("driverId") Long driverId,
//             @Param("passengerId") Long passengerId,
//             @Param("status") RideStatus status,
//             @Param("fromDate") LocalDateTime fromDate,
//             @Param("toDate") LocalDateTime toDate,
//             Pageable pageable);
}

