package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.RideResponse;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/driver/rides")
@RequiredArgsConstructor
public class DriverRideController {
    @PutMapping("/current/start")
    public ResponseEntity<RideResponse> startCurrentRide(Authentication auth) {
        //String driverEmail = auth.getName(); // from JWT
        //RideResponse started = driverRideService.startCurrentRide(driverEmail);
        // Mocked response
        // treba da updatuje status voznje na "IN_PROGRESS" i da postavi vreme pocetka voznje na trenutno vreme, vrati response voznje
        RideResponse started = new RideResponse();
        return ResponseEntity.ok(started);
    }
}
