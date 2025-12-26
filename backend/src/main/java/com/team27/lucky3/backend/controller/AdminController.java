package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.CreateDriver;
import com.team27.lucky3.backend.dto.response.RideResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class AdminController {

    @PostMapping(value = "/drivers", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateDriver> createDriver(
            @Valid @RequestBody CreateDriver requestDTO
    ) {
        // calls service, service call repository to save with status=pending_activation
        // Driver driver = driverService.createDriver(requestDTO);

        //email sending with initial password setup link

        // returnam response dto
        // return new ResponseEntity<>(new DriverCreatedDTO(driver), HttpStatus.CREATED);
        // TODO: proveriti da li je ovo dobro
        return new ResponseEntity<>(requestDTO, HttpStatus.CREATED);
    }

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
        RideResponse r1 = new RideResponse();
        r1.setId(101L);
        r1.setStartTime(LocalDateTime.now().minusHours(2));
        r1.setEndTime(LocalDateTime.now().minusHours(1));
        r1.setTotalCost(1250.00);
        r1.setDriverEmail("driver1@example.com");
        r1.setPassengerEmail("passenger1@example.com");
        r1.setEstimatedTimeInMinutes(45);
        r1.setRideStatus("FINISHED");
        r1.setRejectionReason(null);
        r1.setPanicPressed(false);
        r1.setDeparture("Bulevar oslobođenja 1");
        r1.setDestination("Fruškogorska 1");
        rides.add(r1);

        RideResponse r2 = new RideResponse();
        r2.setId(102L);
        r2.setStartTime(LocalDateTime.now().minusHours(5));
        r2.setEndTime(null);
        r2.setTotalCost(0.00);
        r2.setDriverEmail("driver2@example.com");
        r2.setPassengerEmail("passenger2@example.com");
        r2.setEstimatedTimeInMinutes(0);
        r2.setRideStatus("REJECTED");
        r2.setRejectionReason("Driver unavailable");
        r2.setPanicPressed(false);
        r2.setDeparture("Centar");
        r2.setDestination("Aerodrom");
        rides.add(r2);

        RideResponse r3 = new RideResponse();
        r3.setId(103L);
        r3.setStartTime(LocalDateTime.now().minusDays(1));
        r3.setEndTime(LocalDateTime.now().minusDays(1).plusMinutes(20));
        r3.setTotalCost(450.00);
        r3.setDriverEmail("driver1@example.com");
        r3.setPassengerEmail("passenger3@example.com");
        r3.setEstimatedTimeInMinutes(20);
        r3.setRideStatus("PANIC");
        r3.setRejectionReason("Voznja nije bezbedna");
        r3.setPanicPressed(true);
        r3.setDeparture("Železnička stanica");
        r3.setDestination("Liman 4");
        rides.add(r3);

        Pageable pageable = PageRequest.of(page, size);
        Page<RideResponse> ridePage = new PageImpl<>(rides, pageable, rides.size());

        return ResponseEntity.ok(ridePage);
    }

    @GetMapping("/rides/{id}")
    public ResponseEntity<RideResponse> getRideDetails(@PathVariable @Min(1) Long id) {
        // service.getRide(id);
        RideResponse ride = new RideResponse();
        ride.setId(id);
        ride.setStartTime(LocalDateTime.now().minusHours(2));
        ride.setEndTime(LocalDateTime.now().minusHours(1));
        ride.setTotalCost(1250.00);
        ride.setDriverEmail("driver1@example.com");
        ride.setPassengerEmail("passenger1@example.com");
        ride.setEstimatedTimeInMinutes(45);
        ride.setRideStatus("FINISHED");
        ride.setRejectionReason(null);
        ride.setPanicPressed(false);
        ride.setDeparture("Bulevar oslobođenja 1");
        ride.setDestination("Fruškogorska 1");
        return ResponseEntity.ok(ride);
    }
}
