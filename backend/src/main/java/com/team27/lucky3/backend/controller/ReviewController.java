package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.ReviewRequest;
import com.team27.lucky3.backend.dto.response.DriverReviewAggregateResponse;
import com.team27.lucky3.backend.dto.response.ReviewResponse;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class ReviewController {

    @PostMapping("/rides/{rideId}/reviews")
    public ResponseEntity<ReviewResponse> addReview(@PathVariable @Min(1) Long rideId, @RequestBody ReviewRequest request) {
        if (rideId == 404) throw new ResourceNotFoundException("Ride not found");

        ReviewResponse response = new ReviewResponse(
                1L, rideId, 123L, request.getDriverRating(), request.getVehicleRating(), request.getComment(), LocalDateTime.now()
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/rides/{rideId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviewsForRide(@PathVariable @Min(1) Long rideId) {
        if (rideId == 404) throw new ResourceNotFoundException("Ride not found");

        ReviewResponse r1 = new ReviewResponse(1L, rideId, 123L, 5, 4, "Great ride!", LocalDateTime.now());
        return ResponseEntity.ok(List.of(r1));
    }

    @GetMapping("/drivers/{id}/reviews")
    public ResponseEntity<DriverReviewAggregateResponse> getDriverReviews(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Driver not found");

        DriverReviewAggregateResponse response = new DriverReviewAggregateResponse(id, 4.5, 4.8, 25);
        return ResponseEntity.ok(response);
    }
}
