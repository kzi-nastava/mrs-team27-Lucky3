package com.example.mobile.services;

import com.example.mobile.models.ReviewRequest;
import com.example.mobile.models.ReviewTokenValidationResponse;
import com.example.mobile.models.RideResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ReviewService {

    /** Authenticated review submission (passenger must be logged in). */
    @POST("api/reviews")
    Call<RideResponse.ReviewInfo> createReview(
            @Body ReviewRequest request,
            @Header("Authorization") String token
    );

    /** Validate a review token from an email link (public, no auth). */
    @GET("api/reviews/validate-token")
    Call<ReviewTokenValidationResponse> validateReviewToken(
            @Query("token") String token
    );

    /** Submit a review using a JWT token from an email link (public, no auth). */
    @POST("api/reviews/with-token")
    Call<RideResponse.ReviewInfo> createReviewWithToken(
            @Body ReviewRequest request
    );
}
