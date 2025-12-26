package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.AdditionalRideInfo;
import com.team27.lucky3.backend.dto.request.CreateRide;
import com.team27.lucky3.backend.dto.request.ReasonRequest;
import com.team27.lucky3.backend.dto.request.RideUpdateRequest;
import com.team27.lucky3.backend.dto.response.RideEstimationResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    // 2.1.2 Estimation of ride
    @GetMapping("/estimation")
    public ResponseEntity<RideEstimationResponse> estimateRide(
            @RequestParam String departure,
            @RequestParam String destination) {

        // Mock response
        RideEstimationResponse response = new RideEstimationResponse(15, 650.00);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 2.5 Canceling a ride
    @PutMapping("/{id}")
    public ResponseEntity<RideResponse> updateRideStatus(
            @PathVariable Long id,
            @RequestBody RideUpdateRequest request) {

        // Mock
        String newStatus = request.getStatus();
        String reason = request.getReason();

        RideResponse response = new RideResponse(
                id,
                LocalDateTime.now(),
                newStatus.equals("FINISHED") ? LocalDateTime.now() : null,
                newStatus.equals("FINISHED") ? 450.0 : 0.0,
                "driver@mock.com", "user@mock.com", 0,
                newStatus,
                reason,
                false, "Start", "End"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 2.6.5 Stop the ride in action
    @PutMapping("/{id}/withdraw")
    public ResponseEntity<RideResponse> stopRide(@PathVariable Long id) {
        // Here would be a call to the rideService.stop(id);
        // Mock response
        RideResponse response = new RideResponse(
                id, LocalDateTime.now().minusMinutes(10), LocalDateTime.now(), 420.0,
                "driver@mock.com", "user@mock.com", 10,
                "FINISHED", null, false, "Start", "CurrentLocation"
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Nakon što korisnik izabere putanju, šalje zahtev + dodatne stavke (tip vozila/bebe/ljubimci)
    @PostMapping
    public ResponseEntity<RideResponse> orderRide(
            /*Authentication auth,*/
            @Valid @RequestBody CreateRide request
    ) {
        //String userEmail = auth.getName(); // identitet iz JWT-a
        //RideResponseDTO created = rideService.createAndAssign(userEmail, request);

        RideResponse mocked = new RideResponse();
        mocked.setId(12L);
        mocked.setStartTime(LocalDateTime.of(2025, 12, 26, 19, 30));
        mocked.setEndTime(LocalDateTime.of(2025, 12, 26, 19, 50));
        mocked.setTotalCost(1280.0);
        mocked.setDriverEmail("driver1@gmail.com");
        mocked.setPassengerEmail("passenger1@gmail.com");
        mocked.setEstimatedTimeInMinutes(20L);
        mocked.setRideStatus("ASSIGNED");
        mocked.setRejectionReason(null);
        mocked.setPanicPressed(false);
        mocked.setDeparture("Bulevar Oslobodjenja 1");
        mocked.setDestination("Promenada Novi Sad");

        return ResponseEntity.status(HttpStatus.CREATED).body(mocked);
    }
}