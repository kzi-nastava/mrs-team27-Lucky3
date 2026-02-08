package com.example.mobile.utils;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor that adds JWT Bearer token to all authenticated requests.
 * 
 * This interceptor retrieves the JWT token from SharedPreferences and adds it
 * to the Authorization header in the format: "Bearer <token>"
 */
public class AuthInterceptor implements Interceptor {

    private final SharedPreferencesManager preferencesManager;

    /**
     * Creates an AuthInterceptor with the given SharedPreferencesManager.
     * 
     * @param preferencesManager The SharedPreferencesManager to retrieve the JWT token
     */
    public AuthInterceptor(SharedPreferencesManager preferencesManager) {
        this.preferencesManager = preferencesManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Retrieve the JWT token from SharedPreferences
        String token = preferencesManager.getToken();

        // If no token is available, proceed with the original request
        if (token == null || token.isEmpty()) {
            return chain.proceed(originalRequest);
        }

        // Build a new request with the Authorization header
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();

        return chain.proceed(authenticatedRequest);
    }
}
