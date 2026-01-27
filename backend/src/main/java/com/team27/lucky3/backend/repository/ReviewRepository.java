package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Calculate average driver rating for a specific driver (through ride -> driver relationship)
    @Query("SELECT AVG(r.driverRating) FROM Review r WHERE r.ride.driver.id = :driverId AND r.driverRating > 0")
    Double findAverageDriverRatingByDriverId(@Param("driverId") Long driverId);
    
    // Count total ratings for a driver
    @Query("SELECT COUNT(r) FROM Review r WHERE r.ride.driver.id = :driverId AND r.driverRating > 0")
    Integer countRatingsByDriverId(@Param("driverId") Long driverId);
    
    // Check if a passenger has already reviewed a specific ride
    boolean existsByRideIdAndPassengerId(Long rideId, Long passengerId);
}
