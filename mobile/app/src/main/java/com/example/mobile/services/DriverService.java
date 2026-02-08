package com.example.mobile.services;

import com.example.mobile.models.DriverChangeRequest;
import com.example.mobile.models.DriverChangeRequestCreated;
import com.example.mobile.models.DriverProfileResponse;
import com.example.mobile.models.DriverResponse;
import com.example.mobile.models.DriverStatsResponse;
import com.example.mobile.models.ReviewDriverChangeRequest;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
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
    Call<DriverStatsResponse> getStats(
            @Path("driverId") long driverId,
            @Header("Authorization") String token
    );   /**
     * Get all drivers
     */
    @GET("api/drivers")
    Call<List<DriverResponse>> getAllDrivers(@Header("Authorization") String token);

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

    /**
     * Create new driver account (Admin only)
     * @param request JSON request body as RequestBody
     * @param profileImage Optional profile image
     * @param token Authorization bearer token
     */
    @Multipart
    @POST("api/drivers")
    Call<DriverResponse> createDriver(
            @Part("request") RequestBody request,           // CreateDriverRequest as JSON
            @Part MultipartBody.Part profileImage,          // Optional profile image
            @Header("Authorization") String token
    );

    @GET("/api/driver-change-requests")
    Call<List<DriverChangeRequest>> getDriverChangeRequests(@Query("status") String status, @Header("Authorization") String token);

    @PUT("/api/driver-change-requests/{requestId}/review")
    Call<Void> reviewDriverChangeRequest(@Path("requestId") Long requestId,
                                         @Body ReviewDriverChangeRequest review,
                                         @Header("Authorization") String token);



}
