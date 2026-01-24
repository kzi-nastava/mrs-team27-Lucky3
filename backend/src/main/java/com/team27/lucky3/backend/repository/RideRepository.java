package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long>, JpaSpecificationExecutor<Ride> {
    boolean existsByDriverIdAndStatusIn(Long driverId, List<RideStatus> statuses);

    // Fetch rides finished in the last 24h (or start of day) to calculate working hours
    @Query("SELECT r FROM Ride r WHERE r.driver.id = :driverId AND r.endTime >= :since AND r.status = 'FINISHED'")
    List<Ride> findFinishedRidesByDriverSince(@Param("driverId") Long driverId, @Param("since") LocalDateTime since);

    // Find next scheduled ride for driver
    List<Ride> findByDriverIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(Long driverId, RideStatus status, LocalDateTime startTime);


    @Query("SELECT DISTINCT r.driver.id FROM Ride r " +
            "WHERE r.driver.id IN :driverIds " +
            "AND r.status NOT IN ('FINISHED', 'REJECTED', 'CANCELLED') " +
            "AND ((r.startTime < :endTime AND r.endTime > :startTime))")
    List<Long> findDriversWithRidesInTimeRange(
            @Param("driverIds") List<Long> driverIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
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

    // Check for active rides for a driver

}
