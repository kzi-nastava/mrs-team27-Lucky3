package com.example.mobile.services;

import com.example.mobile.models.LoginRequest;
import com.example.mobile.models.RegistrationRequest;
import com.example.mobile.models.TokenResponse;
import com.example.mobile.models.UserResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit service interface for user authentication and management endpoints.
 * 
 * Base URL: http://<IP_ADDR>:8080/api/
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
    @POST("auth/login")
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
    @POST("auth/register")
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
    @POST("auth/register")
    Call<UserResponse> registerWithoutImage(@Part("data") RequestBody data);

    /**
     * Logout endpoint.
     * POST /api/auth/logout
     * Requires authentication header.
     */
    @POST("auth/logout")
    Call<Void> logout();

    /**
     * Forgot password endpoint.
     * POST /api/auth/forgot-password
     * 
     * @param emailRequest Contains the email address
     */
    @POST("auth/forgot-password")
    Call<Void> forgotPassword(@Body EmailRequest emailRequest);

    /**
     * Reset password endpoint.
     * POST /api/auth/reset-password
     * 
     * @param resetRequest Contains token and new password
     */
    @POST("auth/reset-password")
    Call<Void> resetPassword(@Body PasswordResetRequest resetRequest);

    /**
     * Validate reset password token.
     * GET /api/auth/reset-password/validate
     * 
     * @param token The reset token to validate
     */
    @GET("auth/reset-password/validate")
    Call<Void> validateResetToken(@Query("token") String token);

    /**
     * Activate account endpoint.
     * GET /api/auth/activate
     * 
     * @param token Activation token from email
     */
    @GET("auth/activate")
    Call<Void> activateAccount(@Query("token") String token);

    /**
     * Resend activation email.
     * POST /api/auth/resend-activation
     * 
     * @param emailRequest Contains the email address
     */
    @POST("auth/resend-activation")
    Call<Void> resendActivation(@Body EmailRequest emailRequest);

    // ========================== User Profile Endpoints ==========================

    /**
     * Get current user profile.
     * GET /api/users/me
     * Requires authentication header.
     * 
     * @return UserResponse with current user data
     */
    @GET("users/me")
    Call<UserResponse> getCurrentUser();

    /**
     * Get user by ID.
     * GET /api/users/{id}
     * 
     * @param userId The user ID
     * @return UserResponse
     */
    @GET("users/{id}")
    Call<UserResponse> getUserById(@Path("id") Long userId);

    // ========================== Helper DTOs ==========================

    /**
     * Simple email request DTO.
     */
    class EmailRequest {
        private String email;

        public EmailRequest() {}

        public EmailRequest(String email) {
            this.email = email;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * Password reset request DTO.
     */
    class PasswordResetRequest {
        private String token;
        private String newPassword;

        public PasswordResetRequest() {}

        public PasswordResetRequest(String token, String newPassword) {
            this.token = token;
            this.newPassword = newPassword;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
