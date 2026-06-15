package com.example.mobile.services;

import com.example.mobile.models.ReportResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ReportService {

    @GET("api/reports/{userId}")
    Call<ReportResponse> getReportForUser(
            @Header("Authorization") String token,
            @Path("userId") Long userId,
            @Query("from") String from,
            @Query("to") String to
    );

    @GET("api/reports/user/{email}")
    Call<ReportResponse> getReportForUserByEmail(
            @Header("Authorization") String token,
            @Path("email") String email,
            @Query("from") String from,
            @Query("to") String to
    );

    @GET("api/reports/admin")
    Call<ReportResponse> getGlobalReport(
            @Header("Authorization") String token,
            @Query("from") String from,
            @Query("to") String to,
            @Query("type") String type
    );
}
