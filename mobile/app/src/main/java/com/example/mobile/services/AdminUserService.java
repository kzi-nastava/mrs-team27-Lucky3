package com.example.mobile.services;

import com.example.mobile.models.BlockUserRequest;
import com.example.mobile.models.BlockUserResponse;
import com.example.mobile.models.UserProfile;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit service interface for admin user management (blocking/unblocking).
 */
public interface AdminUserService {

    /**
     * Block a user.
     * POST /api/admin/users/block
     */
    @POST("api/admin/users/block")
    Call<Void> blockUser(
            @Header("Authorization") String token,
            @Body BlockUserRequest request
    );

    /**
     * Unblock a user.
     * POST /api/admin/users/unblock/{email}
     */
    @POST("api/admin/users/unblock/{email}")
    Call<BlockUserResponse> unblockUser(
            @Header("Authorization") String token,
            @Path("email") String email
    );

    /**
     * Get all blocked users.
     * GET /api/admin/users/blocked
     */
    @GET("api/admin/users/blocked")
    Call<List<UserProfile>> getBlockedUsers(
            @Header("Authorization") String token
    );

    /**
     * Get all unblocked users.
     * GET /api/admin/users/unblocked
     */
    @GET("api/admin/users/unblocked")
    Call<List<UserProfile>> getUnblockedUsers(
            @Header("Authorization") String token
    );
}
