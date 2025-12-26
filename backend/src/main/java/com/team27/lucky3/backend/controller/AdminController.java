package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.response.RideResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    // 2.9.3 Overview of ride history
    @GetMapping("/rides")
    public ResponseEntity<Page<RideResponse>> getRidesHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long passengerId)
    {

        List<RideResponse> rides = new ArrayList<>();
        rides.add(new RideResponse(
                101L,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                1250.00,
                "driver1@example.com",
                "passenger1@example.com",
                45,
                "FINISHED",
                null,
                false,
                "Bulevar oslobođenja 1",
                "Fruškogorska 1"
        ));

        rides.add(new RideResponse(
                102L,
                LocalDateTime.now().minusHours(5),
                null,
                0.00,
                "driver2@example.com",
                "passenger2@example.com",
                0,
                "REJECTED",
                "Driver unavailable",
                false,
                "Centar",
                "Aerodrom"
        ));

        rides.add(new RideResponse(
                103L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1).plusMinutes(20),
                450.00,
                "driver1@example.com",
                "passenger3@example.com",
                20,
                "PANIC",
                "Voznja nije bezbedna",
                true,
                "Železnička stanica",
                "Liman 4"
        ));

        Pageable pageable = PageRequest.of(page, size);
        Page<RideResponse> ridePage = new PageImpl<>(rides, pageable, rides.size());

        return new ResponseEntity<>(ridePage, HttpStatus.OK);
    }

    @GetMapping("/rides/{id}")
    public ResponseEntity<RideResponse> getRideDetails(@PathVariable Long id) {
        // service.getRide(id);
        RideResponse ride = new RideResponse(
                id,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                1250.00,
                "driver1@example.com",
                "passenger1@example.com",
                45,
                "FINISHED",
                null,
                false,
                "Bulevar oslobođenja 1",
                "Fruškogorska 1"
        );

        return new ResponseEntity<>(ride, HttpStatus.OK);
    }
}