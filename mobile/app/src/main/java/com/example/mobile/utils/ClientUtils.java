package com.example.mobile.utils;

import com.example.mobile.BuildConfig;
import com.example.mobile.services.DriverService;
import com.example.mobile.services.PanicService;
import com.example.mobile.services.ReviewService;
import com.example.mobile.services.RideService;
import com.example.mobile.services.UserService;
import com.example.mobile.services.VehicleService;
import com.example.mobile.services.AdminService;
import com.example.mobile.services.SupportService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * DateTimeFormatter for LocalDateTime serialization.
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    /**
     * Gson instance with proper configuration.
     */
    /**
     * Gson instance with proper configuration including LocalDateTime support.
     */

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    context.serialize(src.format(DATE_TIME_FORMATTER))
            )
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER)
            )
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
     * Retrofit instance.
     */
    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(SERVICE_API_PATH)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    /**
     * UserService instance.
     */
    public static final UserService userService = retrofit.create(UserService.class);

    /**
     * Driver service instance
     */

    public static final DriverService driverService = retrofit.create(DriverService.class);
    /**
     * VehicleService instance.
     */
    public static final VehicleService vehicleService = retrofit.create(VehicleService.class);

    /**
     * RideService instance.
     */
    public static final RideService rideService = retrofit.create(RideService.class);

    /**
     * PanicService instance (admin-only panic alerts).
     */
    public static final PanicService panicService = retrofit.create(PanicService.class);

    /**
     * ReviewService instance (ride reviews).
     */
    public static final ReviewService reviewService = retrofit.create(ReviewService.class);

    /**
     * AdminService instance (admin pricing endpoints).
     */
    public static final AdminService adminService = retrofit.create(AdminService.class);

    /**
     * SupportService instance (support chat endpoints).
     */
    public static final SupportService supportService = retrofit.create(SupportService.class);

    /**
     * Returns the base Retrofit instance.
     */
    public static Retrofit