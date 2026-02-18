package com.example.mobile.services;

import com.example.mobile.models.NotificationResponseDTO;
import com.example.mobile.models.PageResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit service for notification management endpoints.
 */
public interface NotificationApiService {

    /**
     * Get paginated notification history for the authenticated user.
     *
     * <pre>GET /api/notification?page=0&amp;size=50&amp;sort=timestamp,desc</pre>
     */
    @GET("api/notification")
    Call<PageResponse<NotificationResponseDTO>> getNotifications(
            @Header("Authorization") String token,
            @Query("page") int page,
            @Query("size") int size,
            @Query("sort") String sort);

    /**
     * Mark all notifications as read.
     *
     * <pre>PUT /api/notification/read-all</pre>
     */
    @PUT("api/notification/read-all")
    Call<Map<String, Integer>> markAllAsRead(@Header("Authorization") String token);

    /**
     * Delete a single notification by its backend ID.
     *
     * <pre>DELETE /api/notification/{id}</pre>
     */
    @DELETE("api/notification/{id}")
    Call<Void> deleteNotification(@Header("Authorization") String token, @Path("id") Long id);

    /**
     * Delete all notifications for the authenticated user.
     *
     * <pre>DELETE /api/notification</pre>
     */
    @DELETE("api/notification")
    Call<Map<String, Integer>> deleteAll(@Header("Authorization") String token);
}
