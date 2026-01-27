package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.request.ReviewRequest;
import com.team27.lucky3.backend.dto.response.ReviewResponse;
import com.team27.lucky3.backend.dto.response.ReviewTokenValidationResponse;
import com.team27.lucky3.backend.entity.Review;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.ReviewRepository;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.service.ReviewService;
import com.team27.lucky3.backend.util.ReviewTokenUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final ReviewTokenUtils reviewTokenUtils;

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        User passenger = getCurrentUser();
        // Assume check that user is actually the passenger of the ride, or allow anonymous for now but usually not.

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));

        if (ride.getEndTime() == null) {
            throw new IllegalStateException("Ride has not finished yet.");
        }

        LocalDateTime deadline = ride.getEndTime().plusDays(3);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalStateException("Review period has expired (3 days limit).");
        }

        // Ensure user is the passenger
        if (passenger != null && ride.getPassengers() != null && !ride.getPassengers().contains(passenger)) {
             throw new IllegalStateException("Only passengers of the ride can review it.");
        }

        // Check if passenger already reviewed this ride
        if (passenger != null && reviewRepository.existsByRideIdAndPassengerId(ride.getId(), passenger.getId())) {
            throw new IllegalStateException("You have already submitted a review for this ride.");
        }

        Review review = new Review();
        review.setRide(ride);
        review.setPassenger(passenger);
        review.setDriverRating(request.getDriverRating());
        review.setVehicleRating(request.getVehicleRating());
        review.setComment(request.getComment());
        review.setTimestamp(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);

        Long passengerId = passenger != null ? passenger.getId() : null;

        return new ReviewResponse(
                savedReview.getId(),
                ride.getId(),
                passengerId,
                savedReview.getDriverRating(),
                savedReview.getVehicleRating(),
                savedReview.getComment(),
                savedReview.getTimestamp()
        );
    }

    @Override
    public ReviewTokenValidationResponse validateToken(String token) {
        Claims claims = reviewTokenUtils.validateAndGetClaims(token);
        
        if (claims == null) {
            return null;
        }
        
        Long rideId = claims.get("rideId", Long.class);
        Long passengerId = claims.get("passengerId", Long.class);
        Long driverId = claims.get("driverId", Long.class);
        
        // Verify ride exists and get details
        Ride ride = rideRepository.findById(rideId).orElse(null);
        if (ride == null) {
            return null;
        }
        
        // Get driver name
        String driverName = null;
        if (ride.getDriver() != null) {
            driverName = ride.getDriver().getName();
            if (ride.getDriver().getSurname() != null) {
                driverName += " " + ride.getDriver().getSurname();
            }
        }
        
        // Get addresses
        String pickupAddress = ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : null;
        String dropoffAddress = ride.getEndLocation() != null ? ride.getEndLocation().getAddress() : null;
        
        return new ReviewTokenValidationResponse(
                true,
                rideId,
                passengerId,
                driverId,
                driverName,
                pickupAddress,
                dropoffAddress
        );
    }

    @Override
    @Transactional
    public ReviewResponse createReviewWithToken(ReviewRequest request) {
        // Validate the token
        Claims claims = reviewTokenUtils.validateAndGetClaims(request.getToken());
        
        if (claims == null) {
            throw new IllegalStateException("Invalid or expired review token.");
        }
        
        Long rideId = claims.get("rideId", Long.class);
        Long passengerId = claims.get("passengerId", Long.class);
        
        // Get the ride
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
        
        if (ride.getEndTime() == null) {
            throw new IllegalStateException("Ride has not finished yet.");
        }
        
        // Check 3-day deadline from ride end time
        LocalDateTime deadline = ride.getEndTime().plusDays(3);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalStateException("Review period has expired (3 days limit).");
        }
        
        // Get the passenger
        User passenger = userRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));
        
        // Verify passenger was actually part of this ride
        if (ride.getPassengers() == null || !ride.getPassengers().contains(passenger)) {
            throw new IllegalStateException("You are not a passenger of this ride.");
        }
        
        // Check if passenger already reviewed this ride
        if (reviewRepository.existsByRideIdAndPassengerId(rideId, passengerId)) {
            throw new IllegalStateException("You have already submitted a review for this ride.");
        }
        
        // Create the review
        Review review = new Review();
        review.setRide(ride);
        review.setPassenger(passenger);
        review.setDriverRating(request.getDriverRating());
        review.setVehicleRating(request.getVehicleRating());
        review.setComment(request.getComment());
        review.setTimestamp(LocalDateTime.now());
        
        Review savedReview = reviewRepository.save(review);
        
        System.out.println("Review created for ride " + rideId + " by passenger " + passengerId + 
                ": driver=" + request.getDriverRating() + ", vehicle=" + request.getVehicleRating());
        
        return new ReviewResponse(
                savedReview.getId(),
                rideId,
                passengerId,
                savedReview.getDriverRating(),
                savedReview.getVehicleRating(),
                savedReview.getComment(),
                savedReview.getTimestamp()
        );
    }

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) return null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return null;
    }
}
