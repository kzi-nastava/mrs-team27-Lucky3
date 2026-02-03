package com.example.mobile.services;

import com.example.mobile.models.CreateRideRequest;
import com.example.mobile.models.RideEstimationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RideService {
    @POST("api/rides/estimate")
    Call<RideEstimationResponse> estimateRide(@Body CreateRideRequest request);
}
