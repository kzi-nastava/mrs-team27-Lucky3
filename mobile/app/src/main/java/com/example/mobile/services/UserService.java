package com.example.mobile.services;

import com.example.mobile.models.EmailRequest;
import com.example.mobile.models.LoginRequest;
import com.example.mobile.models.PasswordResetRequest;
import com.example.mobile.models.ProfileUserResponse;
import com.example.mobile.models.RegistrationRequest;
import com.example.mobile.models.TokenResponse;
import com.example.mobile.models.UserResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit service interface for user authentication and management endpoints.
 * 
 * Base URL: http://<IP_ADDR>:8080/
 */
public interface UserService {

    // ========================== Authentication Endpoints ==========================

    /**
     * Login endpoint.
     * POST /api/auth/login
     * 
     * @param loginRequest Contains email and password
     * @return TokenResponse with access and refresh tokens
     */
    @POST("api/auth/login")
    Call<TokenResponse> login(@Body LoginRequest loginRequest);

    /**
     * Register new passenger endpoint.
     * POST /api/auth/register
     * Uses multipart for optional profile image.
     * 
     * @param data RegistrationRequest JSON part
     * @param profileImage Optional profile image file
     * @return UserResponse with registered user data
     */
    @Multipart
    @POST("api/auth/register")
    Call<UserResponse> register(
            @Part("data") RequestBody data,
            @Part MultipartBody.Part profileImage
    );

    /**
     * Register new passenger endpoint (without profile image).
     * POST /api/auth/register
     * 
     * @param data RegistrationRequest JSON part
     * @return UserResponse with registered user data
     */
    @Multipart
    @POST("api/auth/register")
    Call<UserResponse> registerWithoutImage(@Part("data") RequestBody data);

    /**
     * Logout endpoint.
     * POST /api/auth/logout
     * Requires authentication header.
     */
    @POST("api/auth/logout")
    Call<Void> logout(@Header("Authorization") String token);

    /**
     * Forgot password endpoint.
     * POST /api/auth/forgot-password
     * 
     * @param emailRequest Contains the email address
     */
    @POST("api/auth/forgot-password")
    Call<Void> forgotPassword(@Body EmailRequest emailRequest);

    /**
     * Reset password endpoint.
     * POST /api/auth/reset-password
     * 
     * @param resetRequest Contains token and new password
     */
    @POST("api/auth/reset-password")
    Call<Void> resetPassword(@Body PasswordResetRequest resetRequest);

    /**
     * Validate reset password token.
     * GET /api/auth/reset-password/validate
     * 
     * @param token The reset token to validate
     */
    @GET("api/auth/reset-password/validate")
    Call<Void> validateResetToken(@Query("token") String token);

    /**
     * Activate account endpoint.
     * GET /api/auth/activate
     * 
     * @param token Activation token from email
     */
    @GET("api/auth/activate")
    Call<Void> activateAccount(@Query("token") String token);

    /**
     * Resend activation email.
     * POST /api/auth/resend-activation
     * 
     * @param emailRequest Contains the email address
     */
    @POST("api/auth/resend-activation")
    Call<Void> resendActivation(@Body EmailRequest emailRequest);

    // ========================== User Profile Endpoints ==========================

    /**
     * Get user by ID.
     * GET /api/users/{id}
     * 
     * @param userId The user ID
     * @return UserResponse
     */
    @GET("api/users/{id}")
    Call<ProfileUserResponse> getUserById(
            @Path("id") Long userId,
            @Header("Authorization") String token
    );

    //2.3 update user information
    @Multipart
    @PUT("api/users/{id}")
    Call<ProfileUserResponse> updatePersonalInfo(
            @Path("id") Long userId,
            @Part("user") RequestBody userData,  // JSON as RequestBody
            @Part MultipartBody.Part profileImage,  // Optional image
            @Header("Authorization") String token
    );
}
