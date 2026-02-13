package com.team27.lucky3.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.RideStopRequest;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.GlobalExceptionHandler;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.service.RideService;
import com.team27.lucky3.backend.service.impl.CustomUserDetailsService;
import com.team27.lucky3.backend.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RideController PUT /{id}/stop endpoint.
 * Tests HTTP layer: routing, validation, authorization, content negotiation,
 * and proper delegation to the service layer.
 */
@WebMvcTest(RideController.class)
@Import({GlobalExceptionHandler.class, RideControllerStopRideTest.MethodSecurityConfig.class})
class RideControllerStopRideTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RideService rideService;

    @MockBean
    private TokenUtils tokenUtils;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private RideStopRequest validRequest;
    private RideResponse successResponse;

    @BeforeEach
    void setUp() {
        validRequest = new RideStopRequest();
        validRequest.setStopLocation(new LocationDto("Bulevar Oslobodjenja 50", 45.2671, 19.8335));

        successResponse = new RideResponse();
        successResponse.setId(1L);
        successResponse.setStatus(RideStatus.FINISHED);
        successResponse.setStartTime(LocalDateTime.now().minusMinutes(30));
        successResponse.setEndTime(LocalDateTime.now());
        successResponse.setTotalCost(720.0);
        successResponse.setDistanceKm(5.0);
        successResponse.setVehicleType(VehicleType.STANDARD);
        successResponse.setPaid(true);
        successResponse.setPassengersExited(true);
        successResponse.setDeparture(new LocationDto("Start", 45.2500, 19.8200));
        successResponse.setDestination(new LocationDto("Bulevar Oslobodjenja 50", 45.2671, 19.8335));

        UserResponse driverResponse = new UserResponse();
        driverResponse.setId(1L);
        driverResponse.setName("John");
        driverResponse.setSurname("Driver");
        driverResponse.setEmail("driver@example.com");
        successResponse.setDriver(driverResponse);

        UserResponse passengerResponse = new UserResponse();
        passengerResponse.setId(2L);
        passengerResponse.setName("Jane");
        passengerResponse.setSurname("Passenger");
        passengerResponse.setEmail("passenger@example.com");
        successResponse.setPassengers(List.of(passengerResponse));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Happy Path
    // ═══════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/stop - 200 OK for valid request by DRIVER")
    void stopRide_validRequest_returns200() throws Exception {
        when(rideService.stopRide(eq(1L), any(RideStopRequest.class))).thenReturn(successResponse);

        mockMvc.perform(put("/api/rides/1/stop")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.passengersExited").value(true))
                .andExpect(jsonPath("$.totalCost").value(720.0))
                .andExpect(jsonPath("$.distanceKm").value(5.0));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/stop - response contains driver information")
    void stopRide_responseContainsDriverInfo() throws Exception {
        when(rideService.stopRide(eq(1L), any(RideStopRequest.class))).thenReturn(successResponse);

        mockMvc.perform(put("/api/rides/1/stop")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver.id").value(1))
                .andExpect(jsonPath("$.driver.name").value("John"))
                .andExpect(jsonPath("$.driver.email").value("driver@example.com"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/stop - response contains passenger list")
    void stopRide_responseContainsPassengers() throws Exception {
        when(rideService.stopRide(eq(1L), any(RideStopRequest.class))).thenReturn(successResponse);

        mockMvc.perform(put("/api/rides/1/stop")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengers", hasSize(1)))
                .andExpect(jsonPath("$.passengers[0].email").value("passenger@example.com"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/stop - response content type is application/json")
    void stopRide_responseContentType_isJson() throws Exception {
        when(rideService.stopRide(eq(1L), any(RideStopRequest.class))).thenReturn(successResponse);

        mockMvc.perform(put("/api/rides/1/stop")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/stop - delegates to service with correct arguments")
    void stopRide_delegatesToService() throws Exception {
        when(rideService.stopRide(eq(1L), any(RideStopRequest.class))).thenReturn(successResponse);

        mockMvc.perform(put("/api/rides/1/stop")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(rideService, times(1)).stopRide(eq(1L), any(RideStopRequest.class));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Authorization Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Authorization")
    class AuthorizationTests {

        @Test
        @DisplayName("PUT /api/rides/{id}/stop - 401 Unauthorized when not authenticated")
        void stopRide_noAuth_returns401() throws Exception {
            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "PASSENGER")
        @DisplayName("PUT /api/rides/{id}/stop - denied for PASSENGER role (caught by generic handler as 500)")
        void stopRide_passenger_deniedAccess() throws Exception {
            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message", containsString("Access Denied")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PUT /api/rides/{id}/stop - denied for ADMIN role (caught by generic handler as 500)")
        void stopRide_admin_deniedAccess() throws Exception {
            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message", containsString("Access Denied")));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Hardcoded ID = 404 Check (in controller)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/404/stop - 404 Not Found due to hardcoded controller check")
    void stopRide_id404_returnsNotFound() throws Exception {
        mockMvc.perform(put("/api/rides/404/stop")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ride not found"))
                .andExpect(jsonPath("$.path").value("/api/rides/404/stop"));

        verify(rideService, never()).stopRide(anyLong(), any());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Service Exceptions Mapping
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Service exceptions")
    class ServiceExceptionTests {

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 404 when service throws ResourceNotFoundException")
        void stopRide_serviceThrowsNotFound_returns404() throws Exception {
            when(rideService.stopRide(eq(1L), any(RideStopRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Ride not found"));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Ride not found"));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 409 when service throws IllegalStateException")
        void stopRide_serviceThrowsIllegalState_returns409() throws Exception {
            when(rideService.stopRide(eq(1L), any(RideStopRequest.class)))
                    .thenThrow(new IllegalStateException("Ride is not in progress"));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.message").value("Ride is not in progress"));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 409 when driver not assigned (IllegalStateException)")
        void stopRide_driverNotAssigned_returns409() throws Exception {
            when(rideService.stopRide(eq(1L), any(RideStopRequest.class)))
                    .thenThrow(new IllegalStateException("Only the assigned driver can stop this ride"));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Only the assigned driver can stop this ride"));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 409 when user not authenticated (IllegalStateException)")
        void stopRide_notAuthenticated_returns409() throws Exception {
            when(rideService.stopRide(eq(1L), any(RideStopRequest.class)))
                    .thenThrow(new IllegalStateException("User not authenticated"));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("User not authenticated"));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when service throws IllegalArgumentException")
        void stopRide_serviceThrowsIllegalArgument_returns400() throws Exception {
            when(rideService.stopRide(eq(1L), any(RideStopRequest.class)))
                    .thenThrow(new IllegalArgumentException("Invalid stop location"));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid stop location"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Request Body Validation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Request validation")
    class RequestValidationTests {

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - error when request body is missing")
        void stopRide_noBody_returnsError() throws Exception {
            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when stopLocation is null")
        void stopRide_nullStopLocation_returns400() throws Exception {
            RideStopRequest emptyRequest = new RideStopRequest();
            emptyRequest.setStopLocation(null);

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message", containsString("Stop location is required")));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when address is blank")
        void stopRide_blankAddress_returns400() throws Exception {
            RideStopRequest badRequest = new RideStopRequest();
            badRequest.setStopLocation(new LocationDto("", 45.2671, 19.8335));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Address is required")));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when latitude is null")
        void stopRide_nullLatitude_returns400() throws Exception {
            RideStopRequest badRequest = new RideStopRequest();
            badRequest.setStopLocation(new LocationDto("Address", null, 19.8335));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Latitude is required")));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when longitude is null")
        void stopRide_nullLongitude_returns400() throws Exception {
            RideStopRequest badRequest = new RideStopRequest();
            badRequest.setStopLocation(new LocationDto("Address", 45.2671, null));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Longitude is required")));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when latitude exceeds +90")
        void stopRide_latitudeTooHigh_returns400() throws Exception {
            RideStopRequest badRequest = new RideStopRequest();
            badRequest.setStopLocation(new LocationDto("Address", 91.0, 19.8335));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Latitude must be between -90 and 90")));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when latitude below -90")
        void stopRide_latitudeTooLow_returns400() throws Exception {
            RideStopRequest badRequest = new RideStopRequest();
            badRequest.setStopLocation(new LocationDto("Address", -91.0, 19.8335));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Latitude must be between -90 and 90")));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when longitude exceeds +180")
        void stopRide_longitudeTooHigh_returns400() throws Exception {
            RideStopRequest badRequest = new RideStopRequest();
            badRequest.setStopLocation(new LocationDto("Address", 45.0, 181.0));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Longitude must be between -180 and 180")));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when longitude below -180")
        void stopRide_longitudeTooLow_returns400() throws Exception {
            RideStopRequest badRequest = new RideStopRequest();
            badRequest.setStopLocation(new LocationDto("Address", 45.0, -181.0));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Longitude must be between -180 and 180")));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 200 OK at boundary latitude=90")
        void stopRide_boundaryLatitude90_isValid() throws Exception {
            RideStopRequest borderRequest = new RideStopRequest();
            borderRequest.setStopLocation(new LocationDto("North Pole", 90.0, 0.0));

            when(rideService.stopRide(eq(1L), any(RideStopRequest.class))).thenReturn(successResponse);

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(borderRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 200 OK at boundary latitude=-90")
        void stopRide_boundaryLatitudeNeg90_isValid() throws Exception {
            RideStopRequest borderRequest = new RideStopRequest();
            borderRequest.setStopLocation(new LocationDto("South Pole", -90.0, 0.0));

            when(rideService.stopRide(eq(1L), any(RideStopRequest.class))).thenReturn(successResponse);

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(borderRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 200 OK at boundary longitude=180")
        void stopRide_boundaryLongitude180_isValid() throws Exception {
            RideStopRequest borderRequest = new RideStopRequest();
            borderRequest.setStopLocation(new LocationDto("Date Line", 0.0, 180.0));

            when(rideService.stopRide(eq(1L), any(RideStopRequest.class))).thenReturn(successResponse);

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(borderRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 200 OK at boundary longitude=-180")
        void stopRide_boundaryLongitudeNeg180_isValid() throws Exception {
            RideStopRequest borderRequest = new RideStopRequest();
            borderRequest.setStopLocation(new LocationDto("Date Line West", 0.0, -180.0));

            when(rideService.stopRide(eq(1L), any(RideStopRequest.class))).thenReturn(successResponse);

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(borderRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when request body is empty JSON")
        void stopRide_emptyJsonBody_returns400() throws Exception {
            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - error when request body is malformed JSON")
        void stopRide_malformedJson_returnsError() throws Exception {
            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{id}/stop - 400 when multiple validation errors exist")
        void stopRide_multipleValidationErrors_returns400() throws Exception {
            // Both address blank AND latitude null
            String body = "{\"stopLocation\": {\"address\": \"\", \"latitude\": null, \"longitude\": null}}";

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Path Variable Validation (@Min(1))
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Path variable validation")
    class PathVariableTests {

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/0/stop - 400 when id < 1 (@Min(1) violation)")
        void stopRide_idZero_returns400() throws Exception {
            mockMvc.perform(put("/api/rides/0/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/-1/stop - 400 when id is negative")
        void stopRide_negativeId_returns400() throws Exception {
            mockMvc.perform(put("/api/rides/-1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/1/stop - 200 OK when id is exactly 1 (minimum valid)")
        void stopRide_idOne_returns200() throws Exception {
            when(rideService.stopRide(eq(1L), any(RideStopRequest.class))).thenReturn(successResponse);

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("PUT /api/rides/{large_id}/stop - 200 OK with large valid id")
        void stopRide_largeId_returns200() throws Exception {
            when(rideService.stopRide(eq(999999L), any(RideStopRequest.class))).thenReturn(successResponse);

            mockMvc.perform(put("/api/rides/999999/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Error Response Structure
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Error response structure")
    class ErrorResponseStructureTests {

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Error response contains timestamp, status, error, message, and path")
        void errorResponse_containsAllFields() throws Exception {
            when(rideService.stopRide(eq(1L), any(RideStopRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Ride not found"));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Ride not found"))
                    .andExpect(jsonPath("$.path").value("/api/rides/1/stop"));
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("409 error response has correct structure for IllegalStateException")
        void conflictResponse_hasCorrectStructure() throws Exception {
            when(rideService.stopRide(eq(1L), any(RideStopRequest.class)))
                    .thenThrow(new IllegalStateException("Ride is not in progress"));

            mockMvc.perform(put("/api/rides/1/stop")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.message").value("Ride is not in progress"))
                    .andExpect(jsonPath("$.path").value("/api/rides/1/stop"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HTTP Method Validation
    // ═══════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("POST /api/rides/{id}/stop - returns 500 (method not supported, caught by generic handler)")
    void stopRide_postMethod_returns500() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/rides/1/stop")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", containsString("not supported")));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("DELETE /api/rides/{id}/stop - returns 500 (method not supported, caught by generic handler)")
    void stopRide_deleteMethod_returns500() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/rides/1/stop")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", containsString("not supported")));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Service Not Called on Validation Failure
    // ═══════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("Service is not invoked when request body is invalid")
    void stopRide_invalidBody_serviceNotInvoked() throws Exception {
        RideStopRequest badRequest = new RideStopRequest();
        badRequest.setStopLocation(null);

        mockMvc.perform(put("/api/rides/1/stop")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());

        verify(rideService, never()).stopRide(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("Service is not invoked when PASSENGER tries to stop ride")
    void stopRide_forbiddenRole_serviceNotInvoked() throws Exception {
        mockMvc.perform(put("/api/rides/1/stop")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", containsString("Access Denied")));

        verify(rideService, never()).stopRide(anyLong(), any(RideStopRequest.class));
    }
}
