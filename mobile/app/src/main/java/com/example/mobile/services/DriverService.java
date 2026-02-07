package com.example.mobile.services;

import com.example.mobile.models.DriverStatsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/**
 * Service for driver-related API calls.
 */
public interface DriverService {
    
    /**
     * Get driver statistics (earnings, rides completed, rating, online hours)
     */
    @GET("api/drivers/{driverId}/stats")
    Call<DriverStatsResponse> getStats(@Path("driverId") long driverId);

    /**
     * Get all drivers
     */
    @GET("api/drivers")
    Call<List<DriverResponse>> getAllDrivers();

    /*
     * Get driver by id for profile managment
     */
    @GET("api/drivers/{id}")
    Call<DriverProfileResponse> getDriverById(
            @Path("id") Long driverId,
            @Header("Authorization") String token
    );

    //2.3 update user information
    @Multipart
    @PUT("api/drivers/{id}")
    Call<DriverChangeRequestCreated> updateDriverInfo(
            @Path("id") Long driverId,
            @Part("request") RequestBody driverData,  // JSON as RequestBody
            @Part MultipartBody.Part profileImage,  // Optional image
            @Header("Authorization") String token
    );
}
