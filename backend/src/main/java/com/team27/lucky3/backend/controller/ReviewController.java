package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.ReviewRequest;
import com.team27.lucky3.backend.dto.response.ReviewResponse;
import com.team27.lucky3.backend.dto.response.ReviewTokenValidationResponse;
import com.team27.lucky3.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    // 2.8 Rate driver + vehicle (ordering user - authenticated)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Validate a review token and return ride information.
     * This endpoint is public (no authentication required).
     */
    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        ReviewTokenValidationResponse response = reviewService.validateToken(token);
        
        if (response == null) {
            // Token is invalid or expired
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(new ErrorResponse("Review link has expired or is invalid."));
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Submit a review using a JWT token.
     * This endpoint is public (no authentication required).
     */
    @PostMapping(value = "/with-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createReviewWithToken(@Valid @RequestBody ReviewRequest request) {
        try {
            ReviewResponse response = reviewService.createReviewWithToken(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("expired") || e.getMessage().contains("Invalid")) {
                return ResponseEntity.status(HttpStatus.GONE)
                        .body(new ErrorResponse(e.getMessage()));
            } else if (e.getMessage().contains("already submitted")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // Simple error response class
    private record ErrorResponse(String message) {}
}
