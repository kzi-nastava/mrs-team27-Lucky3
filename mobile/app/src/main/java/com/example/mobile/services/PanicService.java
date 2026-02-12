package com.example.mobile.services;

import com.example.mobile.models.PageResponse;
import com.example.mobile.models.PanicResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * Retrofit service interface for panic-related endpoints.
 * Admin-only: GET /api/panic
 */
public interface PanicService {

    /**
     * Get all panic alerts (paginated, admin-only).
     */
    @GET("api/panic")
    Call<PageResponse<PanicResponse>> getPanics(
        @Query("page") int page,
        @Query("size") int size,
        @Header("Authorization") String token
    );
}
