package com.example.mobile.services;

import com.example.mobile.models.CreateRideRequest;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideEstimationResponse;
import com.example.mobile.models.RideResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RideService {
    
    @POST("api/rides/estimate")
    Call<RideEstimationResponse> estimateRide(@Body CreateRideRequest request);
    
    /**
     * Get ride by ID
     */
    @GET("api/rides/{id}")
    Call<RideResponse> getRide(@Path("id") long id, @Header("Authorization") String token);
    
    /**
     * Get ride history with pagination and filters
     */
    @GET("api/rides")
    Call<PageResponse<RideResponse>> getRidesHistory(
        @Query("driverId") Long driverId,
        @Query("passengerId") Long passengerId,
        @Query("status") String status,
        @Query("fromDate") String fromDate,
        @Query("toDate") String toDate,
        @Query("page") Integer page,
        @Query("size") Integer size,
        @Query("sort") String sort,
        @Header("Authorization") String token
    );
}
