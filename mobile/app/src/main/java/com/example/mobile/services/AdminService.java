package com.example.mobile.services;

import com.example.mobile.models.UpdateVehiclePriceRequest;
import com.example.mobile.models.VehiclePriceResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;

/**
 * Retrofit service interface for admin-specific endpoints.
 * All endpoints require ADMIN role authentication.
 */
public interface AdminService {

    /**
     * Get all vehicle prices.
     * GET /api/admin/vehicle-prices
     */
    @GET("api/admin/vehicle-prices")
    Call<List<VehiclePriceResponse>> getAllVehiclePrices(
        @Header("Authorization") String token
    );

    /**
     * Update a vehicle price.
     * PUT /api/admin/vehicle-prices
     */
    @PUT("api/admin/vehicle-prices")
    Call<VehiclePriceResponse> updateVehiclePrice(
        @Header("Authorization") String token,
        @Body UpdateVehiclePriceRequest request
    );
}
