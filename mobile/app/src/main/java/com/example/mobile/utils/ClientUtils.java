package com.example.mobile.utils;

import com.example.mobile.BuildConfig;
import com.example.mobile.services.DriverService;
import com.example.mobile.services.RideService;
import com.example.mobile.services.UserService;
import com.example.mobile.services.VehicleService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ClientUtils - Static Retrofit instance holder (Lecture 07 Compliance).
 * 
 * This class provides centralized access to Retrofit and all API service interfaces.
 * Uses BuildConfig.IP_ADDR for IP address configuration as per course requirements.
 */
public class ClientUtils {

    /**
     * Base API path using BuildConfig for IP configuration.
     * Format: http://<IP_ADDR>:8081/
     * Note: Do not include trailing path segments - Retrofit endpoints handle the rest
     */
    public static final String SERVICE_API_PATH = "http://" + BuildConfig.IP_ADDR + ":8081/";

    /**
     * Connection timeout in seconds.
     */
    private static final int CONNECT_TIMEOUT = 30;

    /**
     * Read timeout in seconds.
     */
    private static final int READ_TIMEOUT = 30;

    /**
     * Write timeout in seconds.
     */
    private static final int WRITE_TIMEOUT = 30;

    /**
     * Gson instance with proper configuration.
     */
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .setLenient()
            .create();

    /**
     * HTTP Logging interceptor for debugging.
     */
    private static final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY);

    /**
     * OkHttpClient without authentication (for login/register).
     */
    private static OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build();

    /**
     * OkHttpClient with authentication interceptor.
     */
    private static OkHttpClient authHttpClient = null;

    /**
     * Retrofit instance without authentication.
     */
    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(SERVICE_API_PATH)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    /**
     * Retrofit instance with authentication (lazy initialized).
     */
    private static Retrofit authRetrofit = null;

    /**
     * UserService instance (without auth - for login/register).
     */
    public static final UserService userService = retrofit.create(UserService.class);

    /**
     * VehicleService instance (without auth - for public vehicle data).
     */
    public static final VehicleService vehicleService = retrofit.create(VehicleService.class);

    /**
     * RideService instance (without auth - for estimation).
     */
    public static final RideService rideService = retrofit.create(RideService.class);

    /**
     * Authenticated UserService instance (lazy initialized).
     */
    private static UserService authenticatedUserService = null;

    /**
     * Authenticated VehicleService instance (lazy initialized).
     */
    private static VehicleService authenticatedVehicleService = null;
    
    /**
     * Authenticated RideService instance (lazy initialized).
     */
    private static RideService authenticatedRideService = null;
    
    /**
     * Authenticated DriverService instance (lazy initialized).
     */
    private static DriverService authenticatedDriverService = null;

    /**
     * Creates and returns an OkHttpClient with the AuthInterceptor.
     * 
     * @param preferencesManager SharedPreferencesManager to retrieve JWT token
     * @return OkHttpClient configured with authentication
     */
    public static OkHttpClient getAuthenticatedClient(SharedPreferencesManager preferencesManager) {
        if (authHttpClient == null) {
            authHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor(preferencesManager))
                    .addInterceptor(loggingInterceptor)
                    .build();
        }
        return authHttpClient;
    }

    /**
     * Creates and returns a Retrofit instance with authentication.
     * 
     * @param preferencesManager SharedPreferencesManager to retrieve JWT token
     * @return Retrofit instance configured with authentication
     */
    public static Retrofit getAuthenticatedRetrofit(SharedPreferencesManager preferencesManager) {
        if (authRetrofit == null) {
            authRetrofit = new Retrofit.Builder()
                    .baseUrl(SERVICE_API_PATH)
                    .client(getAuthenticatedClient(preferencesManager))
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return authRetrofit;
    }

    /**
     * Gets the authenticated UserService.
     * 
     * @param preferencesManager SharedPreferencesManager to retrieve JWT token
     * @return UserService with authentication headers
     */
    public static UserService getAuthenticatedUserService(SharedPreferencesManager preferencesManager) {
        if (authenticatedUserService == null) {
            authenticatedUserService = getAuthenticatedRetrofit(preferencesManager).create(UserService.class);
        }
        return authenticatedUserService;
    }

    /**
     * Gets the authenticated VehicleService.
     * 
     * @param preferencesManager SharedPreferencesManager to retrieve JWT token
     * @return VehicleService with authentication headers
     */
    public static VehicleService getAuthenticatedVehicleService(SharedPreferencesManager preferencesManager) {
        if (authenticatedVehicleService == null) {
            authenticatedVehicleService = getAuthenticatedRetrofit(preferencesManager).create(VehicleService.class);
        }
        return authenticatedVehicleService;
    }
    
    /**
     * Gets the authenticated RideService.
     * 
     * @param preferencesManager SharedPreferencesManager to retrieve JWT token
     * @return RideService with authentication headers
     */
    public static RideService getAuthenticatedRideService(SharedPreferencesManager preferencesManager) {
        if (authenticatedRideService == null) {
            authenticatedRideService = getAuthenticatedRetrofit(preferencesManager).create(RideService.class);
        }
        return authenticatedRideService;
    }
    
    /**
     * Gets the authenticated DriverService.
     * 
     * @param preferencesManager SharedPreferencesManager to retrieve JWT token
     * @return DriverService with authentication headers
     */
    public static DriverService getAuthenticatedDriverService(SharedPreferencesManager preferencesManager) {
        if (authenticatedDriverService == null) {
            authenticatedDriverService = getAuthenticatedRetrofit(preferencesManager).create(DriverService.class);
        }
        return authenticatedDriverService;
    }

    /**
     * Resets authenticated clients (call on logout).
     */
    public static void resetAuthenticatedClients() {
        authHttpClient = null;
        authRetrofit = null;
        authenticatedUserService = null;
        authenticatedVehicleService = null;
        authenticatedRideService = null;
        authenticatedDriverService = null;
    }

    /**
     * Returns the base Retrofit instance (no auth).
     */
    public static Retrofit getRetrofit() {
        return retrofit;
    }

    /**
     * Returns the Gson instance.
     */
    public static Gson getGson() {
        return gson;
    }
}
