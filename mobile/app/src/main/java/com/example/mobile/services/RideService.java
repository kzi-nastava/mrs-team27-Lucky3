package com.example.mobile.services;

import com.example.mobile.models.CreateRideRequest;
import com.example.mobile.models.EndRideRequest;
import com.example.mobile.models.FavoriteRouteRequest;
import com.example.mobile.models.FavoriteRouteResponse;
import com.example.mobile.models.InconsistencyRequest;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideCancellationRequest;
import com.example.mobile.models.RideEstimationResponse;
import com.example.mobile.models.RidePanicRequest;
import com.example.mobile.models.RideResponse;
import com.example.mobile.models.RideStopRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RideService {

    /**
     * Estimate ride fare before booking.
     * POST /api/rides/estimate
     */
    @POST("api/rides/estimate")
    Call<RideEstimationResponse> estimateRide(@Body CreateRideRequest request);

    /**
     * Create a new ride request.
     * POST /api/rides
     */
    @POST("api/rides")
    Call<RideResponse> createRide(
        @Body CreateRideRequest request,
        @Header("Authorization") String token
    );

    /**
     * Get ride by ID.
     * GET /api/rides/{id}
     */
    @GET("api/rides/{id}")
    Call<RideResponse> getRide(
        @Path("id") long id,
        @Header("Authorization") String token
    );

    /**
     * Get ride history with pagination and filters.
     * GET /api/rides
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

    /**
     * Cancel a ride.
     * PUT /api/rides/{id}/cancel
     */
    @PUT("api/rides/{id}/cancel")
    Call<RideResponse> cancelRide(
        @Path("id") long id,
        @Body RideCancellationRequest request,
        @Header("Authorization") String token
    );

    /**
     * Get active rides with pagination and filters.
     * GET /api/rides (filtered by status)
     */
    @GET("api/rides")
    Call<PageResponse<RideResponse>> getActiveRides(
        @Query("driverId") Long driverId,
        @Query("passengerId") Long passengerId,
        @Query("status") String status,
        @Query("page") Integer page,
        @Query("size") Integer size,
        @Header("Authorization") String token
    );

    /**
     * Trigger PANIC button for a ride (driver or passenger).
     * PUT /api/rides/{id}/panic
     */
    @PUT("api/rides/{id}/panic")
    Call<RideResponse> panicRide(
        @Path("id") long id,
        @Body RidePanicRequest request,
        @Header("Authorization") String token
    );

    /**
     * End (finish) a ride normally.
     * PUT /api/rides/{id}/end
     */
    @PUT("api/rides/{id}/end")
    Call<RideResponse> endRide(
        @Path("id") long id,
        @Body EndRideRequest request,
        @Header("Authorization") String token
    );

    /**
     * Stop a ride early at the current vehicle location (driver only).
     * PUT /api/rides/{id}/stop
     */
    @PUT("api/rides/{id}/stop")
    Call<RideResponse> stopRide(
        @Path("id") long id,
        @Body RideStopRequest request,
        @Header("Authorization") String token
    );

    /**
     * Mark an intermediate stop as completed.
     * PUT /api/rides/{id}/stop/{stopIndex}/complete
     */
    @PUT("api/rides/{id}/stop/{stopIndex}/complete")
    Call<RideResponse> completeStop(
        @Path("id") long rideId,
        @Path("stopIndex") int stopIndex,
        @Header("Authorization") String token
    );

    /**
     * Report an inconsistency for a ride.
     * POST /api/rides/{id}/inconsistencies
     */
    @POST("api/rides/{id}/inconsistencies")
    Call<Void> reportInconsistency(
        @Path("id") long rideId,
        @Body InconsistencyRequest request,
        @Header("Authorization") String token
    );

    /**
     * Get favourite routes for a passenger.
     * GET /api/rides/{id}/favourite-routes
     */
    @GET("api/rides/{id}/favourite-routes")
    Call<List<FavoriteRouteResponse>> getFavoriteRoutes(
        @Path("id") Long passengerId,
        @Header("Authorization") String token
    );

    /**
     * Add a favourite route for a passenger.
     * POST /api/rides/{id}/favourite-route
     */
    @POST("api/rides/{id}/favourite-route")
    Call<Void> addFavouriteRoute(
        @Path("id") Long passengerId,
        @Body FavoriteRouteRequest request,
        @Header("Authorization") String token
    );

    /**
     * Remove a favourite route for a passenger.
     * DELETE /api/rides/{passengerId}/favourite-routes/{favouriteRouteId}
     */
    @DELETE("api/rides/{passengerId}/favourite-routes/{favouriteRouteId}")
    Call<Void> removeFavouriteRoute(
        @Path("passengerId") Long passengerId,
        @Path("favouriteRouteId") Long favouriteRouteId,
        @Header("Authorization") String token
    );
}
