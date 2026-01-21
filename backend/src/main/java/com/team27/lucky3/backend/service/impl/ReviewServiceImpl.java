package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.request.ReviewRequest;
import com.team27.lucky3.backend.dto.response.ReviewResponse;
import com.team27.lucky3.backend.entity.Review;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.ReviewRepository;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.service.ReviewService;
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

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) return null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return null;
    }
}
