package com.example.mobile.services;

import com.example.mobile.models.LocationDto;
import com.example.mobile.models.SimulationLockResponse;
import com.example.mobile.models.VehicleLocationResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

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

    /**
     * Update a vehicle's current location.
     * PUT /api/vehicles/{id}/location
     */
    @PUT("api/vehicles/{id}/location")
    Call<Void> updateVehicleLocation(
            @Path("id") long vehicleId,
            @Body LocationDto location,
            @Header("Authorization") String token);

    /**
     * Acquire or refresh a simulation lock for a vehicle.
     * Only one client (web tab or mobile app) should drive the vehicle at a time.
     * PUT /api/vehicles/{id}/simulation-lock
     */
    @PUT("api/vehicles/{id}/simulation-lock")
    Call<SimulationLockResponse> acquireSimulationLock(
            @Path("id") long vehicleId,
            @Body Map<String, String> body,
            @Header("Authorization") String token);

    /**
     * Release a simulation lock for a vehicle.
     * DELETE /api/vehicles/{id}/simulation-lock
     */
    @HTTP(method = "DELETE", path = "api/vehicles/{id}/simulation-lock", hasBody = true)
    Call<Void> releaseSimulationLock(
            @Path("id") long vehicleId,
            @Body Map<String, String> body,
            @Header("Authorization") String token);
}
