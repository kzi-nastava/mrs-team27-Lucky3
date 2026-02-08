package com.example.mobile.services;

import com.example.mobile.models.VehicleLocationResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit service interface for vehicle-related endpoints.
 * 
 * Base URL: http://<IP_ADDR>:8081/
 */
public interface VehicleService {

    /**
     * Get all active vehicles with their current location and availability status.
     * GET /api/vehicles/active
     * 
     * @return List of VehicleLocationResponse with vehicle details
     */
    @GET("api/vehicles/active")
    Call<List<VehicleLocationResponse>> getActiveVehicles();
}
