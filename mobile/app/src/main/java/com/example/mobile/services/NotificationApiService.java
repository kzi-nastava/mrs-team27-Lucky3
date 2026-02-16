package com.example.mobile.services;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Header;

/**
 * Retrofit service for notification management endpoints.
 */
public interface NotificationApiService {

    /**
     * Delete all notifications for the authenticated user.
     *
     * <pre>DELETE /api/notification</pre>
     */
    @DELETE("api/notification")
    Call<Map<String, Integer>> deleteAll(@Header("Authorization") String token);
}
