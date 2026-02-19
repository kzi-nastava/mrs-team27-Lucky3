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

    // Find next scheduled or pending rides for driver
    List<Ride> findByDriverIdAndStatusInOrderByStartTimeAsc(Long driverId, List<RideStatus> statuses);


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
    
    // Count completed rides for a driver
    @Query("SELECT COUNT(r) FROM Ride r WHERE r.driver.id = :driverId AND r.status = 'FINISHED'")
    Integer countCompletedRidesByDriverId(@Param("driverId") Long driverId);
    
    // Sum total earnings for a driver from completed rides
    @Query("SELECT COALESCE(SUM(r.totalCost), 0) FROM Ride r WHERE r.driver.id = :driverId AND r.status = 'FINISHED'")
    Double sumTotalEarningsByDriverId(@Param("driverId") Long driverId);
    
    // Find all rides that are currently in progress (for cost tracking)
    @Query("SELECT r FROM Ride r WHERE r.status IN ('IN_PROGRESS', 'ACTIVE') AND r.driver IS NOT NULL")
    List<Ride> findAllInProgressRides();

    // Count all active rides (PENDING, ACCEPTED, SCHEDULED, IN_PROGRESS, ACTIVE)
    @Query("SELECT COUNT(r) FROM Ride r WHERE r.status IN ('PENDING', 'ACCEPTED', 'SCHEDULED', 'IN_PROGRESS', 'ACTIVE')")
    Integer countActiveRides();

    // Get all rides with active statuses for passenger counting
    @Query("SELECT r FROM Ride r WHERE r.status IN ('IN_PROGRESS', 'ACTIVE')")
    List<Ride> findAllActiveRidesForPassengerCount();

    // Find rides scheduled to start within a time window (for reminder notifications)
    @Query("SELECT r FROM Ride r WHERE r.scheduledTime BETWEEN :start AND :end AND r.status IN :statuses")
    List<Ride> findByScheduledTimeBetweenAndStatusIn(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<RideStatus> statuses
    );
//____________________________________________________________________________________________________________________________________________________________
// this is used for reports

    // Find rides for a specific driver
    List<Ride> findByDriverIdAndStartTimeBetweenAndStatus(
            Long driverId,
            LocalDateTime from,
            LocalDateTime to,
            RideStatus status);

    // Find rides for a specific passenger using the ride_passengers join table
    @Query(value = "SELECT r.* FROM ride r " +
            "INNER JOIN ride_passengers rp ON r.id = rp.ride_id " +
            "WHERE rp.user_id = :passengerId " +
            "AND r.start_time BETWEEN :from AND :to " +
            "AND r.status = :status",
            nativeQuery = true)
    List<Ride> findRidesForPassenger(
            @Param("passengerId") Long passengerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") String status);

    // Find all rides in date range
    List<Ride> findByStartTimeBetweenAndStatus(
            LocalDateTime from,
            LocalDateTime to,
            RideStatus status);
}


