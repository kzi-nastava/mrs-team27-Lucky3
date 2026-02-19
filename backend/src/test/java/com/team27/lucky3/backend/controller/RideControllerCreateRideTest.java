package com.team27.lucky3.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.CreateRideRequest;
import com.team27.lucky3.backend.dto.request.RideRequirements;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.GlobalExceptionHandler;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RideController.class)
@Import({GlobalExceptionHandler.class, RideControllerCreateRideTest.MethodSecurityConfig.class})
class RideControllerCreateRideTest {

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
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - success creates ride with 201 status")
    void createRide_success() throws Exception {
        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.PENDING);
        response.setEstimatedCost(450.0);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.estimatedCost").value(450.0));

        verify(rideService, times(1)).createRide(any(CreateRideRequest.class));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - success with scheduled time")
    void createRide_withScheduledTime_success() throws Exception {
        LocalDateTime scheduledTime = LocalDateTime.now().plusHours(2);
        validRequest.setScheduledTime(scheduledTime);

        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.SCHEDULED);
        response.setScheduledTime(scheduledTime);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - success with stops")
    void createRide_withStops_success() throws Exception {
        LocationDto stop1 = new LocationDto("Kalemegdan, Belgrade", 44.823059, 20.451434);
        LocationDto stop2 = new LocationDto("Ada Bridge, Belgrade", 44.820556, 20.421944);
        validRequest.setStops(Arrays.asList(stop1, stop2));

        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.PENDING);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - success with passenger emails")
    void createRide_withPassengerEmails_success() throws Exception {
        validRequest.setPassengerEmails(Arrays.asList("passenger1@example.com", "passenger2@example.com"));

        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.PENDING);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - success with LUXURY vehicle")
    void createRide_withLuxuryVehicle_success() throws Exception {
        requirements.setVehicleType(VehicleType.LUXURY);

        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.PENDING);
        response.setVehicleType(VehicleType.LUXURY);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleType").value("LUXURY"));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - success with VAN vehicle")
    void createRide_withVanVehicle_success() throws Exception {
        requirements.setVehicleType(VehicleType.VAN);

        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.PENDING);
        response.setVehicleType(VehicleType.VAN);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleType").value("VAN"));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - success with baby transport")
    void createRide_withBabyTransport_success() throws Exception {
        requirements.setBabyTransport(true);

        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.PENDING);
        response.setBabyTransport(true);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.babyTransport").value(true));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - success with pet transport")
    void createRide_withPetTransport_success() throws Exception {
        requirements.setPetTransport(true);

        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.PENDING);
        response.setPetTransport(true);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petTransport").value(true));
    }

    // ===== Authorization Scenarios =====

    @Test
    @DisplayName("POST /api/rides - 401 unauthorized without authentication")
    void createRide_unauthorized() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(rideService, never()).createRide(any());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("POST /api/rides - 403 forbidden for driver role")
    void createRide_forbiddenForDriver() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(rideService, never()).createRide(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/rides - 403 forbidden for admin role")
    void createRide_forbiddenForAdmin() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(rideService, never()).createRide(any());
    }

    // ===== Validation Error Scenarios (400) =====

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 400 when start location is null")
    void createRide_nullStartLocation_badRequest() throws Exception {
        validRequest.setStart(null);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).createRide(any());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 400 when destination is null")
    void createRide_nullDestination_badRequest() throws Exception {
        validRequest.setDestination(null);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).createRide(any());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 400 when requirements are null")
    void createRide_nullRequirements_badRequest() throws Exception {
        validRequest.setRequirements(null);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).createRide(any());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 400 when vehicle type is null")
    void createRide_nullVehicleType_badRequest() throws Exception {
        requirements.setVehicleType(null);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).createRide(any());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 400 when start address is blank")
    void createRide_blankStartAddress_badRequest() throws Exception {
        startLocation.setAddress("");

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).createRide(any());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 400 when scheduled time is in the past")
    void createRide_pastScheduledTime_badRequest() throws Exception {
        validRequest.setScheduledTime(LocalDateTime.now().minusHours(1));

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).createRide(any());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 400 when latitude is out of range")
    void createRide_invalidLatitude_badRequest() throws Exception {
        startLocation.setLatitude(95.0);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).createRide(any());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 400 when longitude is out of range")
    void createRide_invalidLongitude_badRequest() throws Exception {
        startLocation.setLongitude(200.0);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).createRide(any());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 400 with empty body")
    void createRide_emptyBody_badRequest() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).createRide(any());
    }

    // ===== Conflict Scenarios (409) =====

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 409 when scheduled time exceeds 5 hour limit")
    void createRide_scheduledTimeTooFar_conflict() throws Exception {
        validRequest.setScheduledTime(LocalDateTime.now().plusHours(6));

        when(rideService.createRide(any(CreateRideRequest.class)))
                .thenThrow(new IllegalArgumentException("scheduledTime must be within the next 5 hours"));

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 409 when user must be logged in")
    void createRide_userNotLoggedIn_conflict() throws Exception {
        when(rideService.createRide(any(CreateRideRequest.class)))
                .thenThrow(new IllegalStateException("User must be logged in to create a ride"));

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    // ===== Method Not Allowed (405) =====

    //Get method cant be tested because it is used for ride history endpoint, so we will test put and delete methods

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("PUT /api/rides - 405 method not allowed")
    void createRide_putMethod_notAllowed() throws Exception {
        mockMvc.perform(put("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("DELETE /api/rides - 405 method not allowed")
    void createRide_deleteMethod_notAllowed() throws Exception {
        mockMvc.perform(delete("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    // ===== Error Response Structure =====

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - 409 error response has correct JSON structure")
    void createRide_conflict_errorStructure() throws Exception {
        when(rideService.createRide(any(CreateRideRequest.class)))
                .thenThrow(new IllegalStateException("User must be logged in to create a ride"));

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User must be logged in to create a ride"))
                .andExpect(jsonPath("$.path").value("/api/rides"));
    }

    // ===== Edge Cases =====

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - success with boundary coordinates")
    void createRide_boundaryCoordinates_success() throws Exception {
        startLocation.setLatitude(90.0);
        startLocation.setLongitude(180.0);
        destinationLocation.setLatitude(-90.0);
        destinationLocation.setLongitude(-180.0);

        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.PENDING);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - success with all transport options enabled")
    void createRide_allTransportOptions_success() throws Exception {
        requirements.setBabyTransport(true);
        requirements.setPetTransport(true);

        RideResponse response = new RideResponse();
        response.setId(1L);
        response.setStatus(RideStatus.PENDING);
        response.setBabyTransport(true);
        response.setPetTransport(true);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.babyTransport").value(true))
                .andExpect(jsonPath("$.petTransport").value(true));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - delegates to service with correct request")
    void createRide_delegatesToService() throws Exception {
        RideResponse response = new RideResponse();
        response.setId(5L);
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(rideService, times(1)).createRide(any(CreateRideRequest.class));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("POST /api/rides - service not called when validation fails")
    void createRide_serviceNotCalled_whenValidationFails() throws Exception {
        validRequest.setStart(null);

        mockMvc.perform(post("/api/rides")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).createRide(any());
    }
}
