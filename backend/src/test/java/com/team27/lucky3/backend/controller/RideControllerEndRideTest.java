package com.team27.lucky3.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team27.lucky3.backend.dto.request.EndRideRequest;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.exception.GlobalExceptionHandler;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.service.RideService;
import com.team27.lucky3.backend.service.impl.CustomUserDetailsService;
import com.team27.lucky3.backend.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RideController.class)
@Import({GlobalExceptionHandler.class, RideControllerEndRideTest.MethodSecurityConfig.class})
class RideControllerEndRideTest {

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

    private EndRideRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new EndRideRequest();
        validRequest.setPaid(true);
        validRequest.setPassengersExited(true);
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - success")
    void endRide_success() throws Exception {
        RideResponse response = new RideResponse();
        response.setId(1L);
        when(rideService.endRide(eq(1L), any(EndRideRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("PUT /api/rides/{id}/end - forbidden for passenger")
    void endRide_forbiddenForPassenger() throws Exception {
        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/rides/{id}/end - forbidden for admin")
    void endRide_forbiddenForAdmin() throws Exception {
        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/rides/{id}/end - unauthorized without user")
    void endRide_unauthorized() throws Exception {
        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 400 bad request for invalid ID (0)")
    void endRide_invalidIdZero() throws Exception {
        mockMvc.perform(put("/api/rides/0/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 400 bad request for negative ID")
    void endRide_negativeId() throws Exception {
        mockMvc.perform(put("/api/rides/-1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("POST /api/rides/{id}/end - 405 method not allowed")
    void endRide_postMethod_notAllowed() throws Exception {
        mockMvc.perform(post("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 404 not found")
    void endRide_notFound() throws Exception {
        when(rideService.endRide(eq(999L), any(EndRideRequest.class)))
                .thenThrow(new ResourceNotFoundException("Ride not found"));

        mockMvc.perform(put("/api/rides/999/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 409 conflict when invalid status")
    void endRide_conflict() throws Exception {
        when(rideService.endRide(eq(1L), any(EndRideRequest.class)))
                .thenThrow(new IllegalStateException("Ride is not in progress"));

        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 400 bad request for invalid body (paid null)")
    void endRide_badRequest_paidNull() throws Exception {
        validRequest.setPaid(null);

        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 400 bad request for invalid body (passengersExited null)")
    void endRide_badRequest_passengersExitedNull() throws Exception {
        validRequest.setPassengersExited(null);

        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 400 bad request for empty body")
    void endRide_badRequest_emptyBody() throws Exception {
        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - delegates to service with correct ID")
    void endRide_delegatesToService() throws Exception {
        RideResponse response = new RideResponse();
        response.setId(5L);
        when(rideService.endRide(eq(5L), any(EndRideRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/rides/5/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(rideService, times(1)).endRide(eq(5L), any(EndRideRequest.class));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - service not called when body is invalid")
    void endRide_serviceNotCalled_whenBodyInvalid() throws Exception {
        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rideService, never()).endRide(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    @DisplayName("PUT /api/rides/{id}/end - service not called when role is forbidden")
    void endRide_serviceNotCalled_whenForbiddenRole() throws Exception {
        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(rideService, never()).endRide(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 404 error response has correct JSON structure")
    void endRide_notFound_errorStructure() throws Exception {
        when(rideService.endRide(eq(999L), any(EndRideRequest.class)))
                .thenThrow(new ResourceNotFoundException("Ride not found"));

        mockMvc.perform(put("/api/rides/999/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ride not found"))
                .andExpect(jsonPath("$.path").value("/api/rides/999/end"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 409 error response has correct JSON structure")
    void endRide_conflict_errorStructure() throws Exception {
        when(rideService.endRide(eq(1L), any(EndRideRequest.class)))
                .thenThrow(new IllegalStateException("Ride is not in progress"));

        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Ride is not in progress"))
                .andExpect(jsonPath("$.path").value("/api/rides/1/end"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("DELETE /api/rides/{id}/end - 405 method not allowed")
    void endRide_deleteMethod_notAllowed() throws Exception {
        mockMvc.perform(delete("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 200 OK with paid=false and passengersExited=false")
    void endRide_bothFlagsFalse_returns200() throws Exception {
        EndRideRequest falseRequest = new EndRideRequest();
        falseRequest.setPaid(false);
        falseRequest.setPassengersExited(false);

        RideResponse response = new RideResponse();
        response.setId(1L);
        when(rideService.endRide(eq(1L), any(EndRideRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(falseRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - large valid ID returns 200")
    void endRide_largeId_returns200() throws Exception {
        RideResponse response = new RideResponse();
        response.setId(999999L);
        when(rideService.endRide(eq(999999L), any(EndRideRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/rides/999999/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(999999));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("GET /api/rides/{id}/end - 405 method not allowed")
    void endRide_getMethod_notAllowed() throws Exception {
        mockMvc.perform(get("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 400 for non-numeric ID")
    void endRide_nonNumericId_badRequest() throws Exception {
        mockMvc.perform(put("/api/rides/abc/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 400 when both fields null")
    void endRide_bothFieldsNull_badRequest() throws Exception {
        EndRideRequest nullRequest = new EndRideRequest();
        nullRequest.setPaid(null);
        nullRequest.setPassengersExited(null);

        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 400 for missing request body")
    void endRide_missingBody_badRequest() throws Exception {
        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - 400 error response has correct JSON structure")
    void endRide_badRequest_errorStructure() throws Exception {
        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - response content type is JSON")
    void endRide_responseContentType_isJson() throws Exception {
        RideResponse response = new RideResponse();
        response.setId(1L);
        when(rideService.endRide(eq(1L), any(EndRideRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    @DisplayName("PUT /api/rides/{id}/end - service receives correct request values")
    void endRide_serviceReceivesCorrectValues() throws Exception {
        RideResponse response = new RideResponse();
        response.setId(1L);
        when(rideService.endRide(eq(1L), any(EndRideRequest.class))).thenReturn(response);

        EndRideRequest request = new EndRideRequest();
        request.setPaid(false);
        request.setPassengersExited(true);

        mockMvc.perform(put("/api/rides/1/end")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        ArgumentCaptor<EndRideRequest> captor = ArgumentCaptor.forClass(EndRideRequest.class);
        verify(rideService).endRide(eq(1L), captor.capture());
        assertFalse(captor.getValue().getPaid());
        assertTrue(captor.getValue().getPassengersExited());
    }
}
