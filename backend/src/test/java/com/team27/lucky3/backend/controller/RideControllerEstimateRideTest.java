package com.team27.lucky3.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team27.lucky3.backend.config.WebSecurityConfig;
import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.CreateRideRequest;
import com.team27.lucky3.backend.dto.request.RideRequirements;
import com.team27.lucky3.backend.dto.response.RideEstimationResponse;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.GlobalExceptionHandler;
import com.team27.lucky3.backend.service.RideService;
import com.team27.lucky3.backend.service.impl.CustomUserDetailsService;
import com.team27.lucky3.backend.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RideController.class)
@Import({GlobalExceptionHandler.class, RideControllerEstimateRideTest.MethodSecurityConfig.class})
class RideControllerEstimateRideTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/rides/estimate").permitAll() // Allow this specific endpoint
                            .anyRequest().authenticated()
                    );
            return http.build();
        }
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

    private CreateRideRequest validRequest;
    private LocationDto startLocation;
    private LocationDto destinationLocation;
    private RideRequirements requirements;

    @BeforeEach
    void setUp() {
        // Setup valid locations
        startLocation = new LocationDto("Main Street 1, Belgrade", 44.787197, 20.457273);
        destinationLocation = new LocationDto("Republic Square, Belgrade", 44.816231, 20.460341);

        // Setup ride requirements
        requirements = new RideRequirements();
        requirements.setVehicleType(VehicleType.STANDARD);
        requirements.setBabyTransport(false);
        requirements.setPetTransport(false);

        // Setup valid request
        validRequest = new CreateRideRequest();
        validRequest.setStart(startLocation);
        validRequest.setDestination(destinationLocation);
        validRequest.setRequirements(requirements);
    }

    // ===== Success Scenarios =====

    @Test
    @DisplayName("POST /api/rides/estimate - success with valid request")
    void estimateRide_success() throws Exception {
        RideEstimationResponse response = new RideEstimationResponse(15, 450.0, 5, 5.2, null);
        when(rideService.estimateRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedTimeInMinutes").value(15))
                .andExpect(jsonPath("$.estimatedCost").value(450.0))
                .andExpect(jsonPath("$.estimatedDriverArrivalInMinutes").value(5))
                .andExpect(jsonPath("$.estimatedDistance").value(5.2));

        verify(rideService, times(1)).estimateRide(any(CreateRideRequest.class));
    }

    @Test
    @DisplayName("POST /api/rides/estimate - success with stops")
    void estimateRide_withStops_success() throws Exception {
        LocationDto stop1 = new LocationDto("Kalemegdan, Belgrade", 44.823059, 20.451434);
        LocationDto stop2 = new LocationDto("Ada Bridge, Belgrade", 44.820556, 20.421944);
        validRequest.setStops(Arrays.asList(stop1, stop2));

        RideEstimationResponse response = new RideEstimationResponse(25, 750.0, 5, 8.5, null);
        when(rideService.estimateRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedDistance").value(8.5));
    }

    @Test
    @DisplayName("POST /api/rides/estimate - accessible without authentication")
    void estimateRide_noAuth_success() throws Exception {
        RideEstimationResponse response = new RideEstimationResponse(15, 450.0, 5, 5.2, null);
        when(rideService.estimateRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // ===== Validation Error Scenarios (400) =====

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when start location is null")
    void estimateRide_nullStartLocation_badRequest() throws Exception {
        validRequest.setStart(null);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when destination is null")
    void estimateRide_nullDestination_badRequest() throws Exception {
        validRequest.setDestination(null);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when requirements are null")
    void estimateRide_nullRequirements_badRequest() throws Exception {
        validRequest.setRequirements(null);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when vehicle type is null")
    void estimateRide_nullVehicleType_badRequest() throws Exception {
        requirements.setVehicleType(null);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when start address is blank")
    void estimateRide_blankStartAddress_badRequest() throws Exception {
        startLocation.setAddress("");

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when start latitude is null")
    void estimateRide_nullStartLatitude_badRequest() throws Exception {
        startLocation.setLatitude(null);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when start longitude is null")
    void estimateRide_nullStartLongitude_badRequest() throws Exception {
        startLocation.setLongitude(null);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when latitude is out of range (> 90)")
    void estimateRide_latitudeOutOfRange_badRequest() throws Exception {
        startLocation.setLatitude(91.0);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when latitude is out of range (< -90)")
    void estimateRide_latitudeNegativeOutOfRange_badRequest() throws Exception {
        startLocation.setLatitude(-91.0);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when longitude is out of range (> 180)")
    void estimateRide_longitudeOutOfRange_badRequest() throws Exception {
        startLocation.setLongitude(181.0);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when longitude is out of range (< -180)")
    void estimateRide_longitudeNegativeOutOfRange_badRequest() throws Exception {
        startLocation.setLongitude(-181.0);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when destination address is blank")
    void estimateRide_blankDestinationAddress_badRequest() throws Exception {
        destinationLocation.setAddress("   ");

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - 400 with empty body")
    void estimateRide_emptyBody_badRequest() throws Exception {
        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).estimateRide(any());
    }

    // ===== Method Not Allowed (405) =====

    @Test
    @DisplayName("GET /api/rides/estimate - 405 method not allowed")
    void estimateRide_getMethod_notAllowed() throws Exception {
        mockMvc.perform(get("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("PUT /api/rides/estimate - 405 method not allowed")
    void estimateRide_putMethod_notAllowed() throws Exception {
        mockMvc.perform(put("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("DELETE /api/rides/estimate - 405 method not allowed")
    void estimateRide_deleteMethod_notAllowed() throws Exception {
        mockMvc.perform(delete("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    // ===== Service Error Scenarios =====

    @Test
    @DisplayName("POST /api/rides/estimate - 400 when service throws IllegalArgumentException")
    void estimateRide_serviceThrowsIllegalArgument_badRequest() throws Exception {
        when(rideService.estimateRide(any(CreateRideRequest.class)))
                .thenThrow(new IllegalArgumentException("Start and destination locations must be provided"));

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ===== Edge Cases =====

    @Test
    @DisplayName("POST /api/rides/estimate - success with boundary latitude values")
    void estimateRide_boundaryLatitude_success() throws Exception {
        startLocation.setLatitude(90.0);
        destinationLocation.setLatitude(-90.0);

        RideEstimationResponse response = new RideEstimationResponse(15, 450.0, 5, 5.2, null);
        when(rideService.estimateRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - success with boundary longitude values")
    void estimateRide_boundaryLongitude_success() throws Exception {
        startLocation.setLongitude(180.0);
        destinationLocation.setLongitude(-180.0);

        RideEstimationResponse response = new RideEstimationResponse(15, 450.0, 5, 5.2, null);
        when(rideService.estimateRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/rides/estimate - success with empty stops list")
    void estimateRide_emptyStops_success() throws Exception {
        validRequest.setStops(Arrays.asList());

        RideEstimationResponse response = new RideEstimationResponse(15, 450.0, 5, 5.2, null);
        when(rideService.estimateRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
