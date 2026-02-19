package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.ReviewRequest;
import com.team27.lucky3.backend.dto.response.ReviewResponse;
import com.team27.lucky3.backend.dto.response.ReviewTokenValidationResponse;

public interface ReviewService {
    ReviewResponse createReview(ReviewRequest request);
    
    /**
     * Validates a review token and returns information about the ride.
     * @param token The JWT token from the review link
     * @return Validation response with ride info, or null if token is invalid/expired
     */
    ReviewTokenValidationResponse validateToken(String token);
    
    /**
     * Creates a review using a JWT token instead of authentication.
     * @param request The review request with token field set
     * @return The created review response
     */
    ReviewResponse createReviewWithToken(ReviewRequest request);
}
