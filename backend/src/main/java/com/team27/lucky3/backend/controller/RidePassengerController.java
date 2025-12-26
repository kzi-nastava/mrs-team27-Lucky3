package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.AddRidePassengers;
import com.team27.lucky3.backend.dto.request.ReasonRequest;
import com.team27.lucky3.backend.dto.request.RideUpdateRequest;
import com.team27.lucky3.backend.dto.response.RideEstimationResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RidePassengerController {
    // dodaje ulinkovanje putnike na voznju, U BATCHU, NE POJEDINACNO
    @PostMapping("/{rideId}/passengers")
    public ResponseEntity<AddRidePassengers> addPassengerToRide(
            @PathVariable Long rideId,
            @RequestBody AddRidePassengers passengers) {

        //Mock data - kasnije ide servis
        return new ResponseEntity<>(passengers, HttpStatus.OK);
    }


}
