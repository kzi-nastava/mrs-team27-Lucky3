package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.AddRidePassengers;
import com.team27.lucky3.backend.dto.request.CreateRide;
import com.team27.lucky3.backend.dto.request.InconsistencyRequest;
import com.team27.lucky3.backend.dto.request.PanicRequest;
import com.team27.lucky3.backend.dto.request.RideEndRequest;
import com.team27.lucky3.backend.dto.request.RideUpdateRequest;
import com.team27.lucky3.backend.dto.response.LocationResponse;
import com.team27.lucky3.backend.dto.response.RideEstimationResponse;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.dto.response.RoutePointResponse;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/rides", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class RideController {

    // 2.1.2 Estimation of ride
    @GetMapping("/estimation")
    public ResponseEntity<RideEstimationResponse> estimateRide(
            @RequestParam String departure,
            @RequestParam String destination) {

        // Mock response
        RideEstimationResponse response = new RideEstimationResponse(15, 650.00);
        return ResponseEntity.ok(response);
    }

    // 2.5 Canceling a ride / Update status
    @PutMapping("/{id}")
    public ResponseEntity<RideResponse> updateRideStatus(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody RideUpdateRequest request) {

        // Mock
        String newStatus = request.getStatus();
        String reason = request.getReason();

        RideResponse response = createDummyRideResponse(id, 10L, 123L, RideStatus.valueOf(newStatus));
        // Note: createDummyRideResponse might need adjustment to handle reason if I want to be precise, but for now reusing it is fine or I can create a new one inline like in src-MUSTHAVE

        return ResponseEntity.ok(response);
    }

    // 2.6.5 Stop the ride in action
    @PutMapping("/{id}/withdraw")
    public ResponseEntity<RideResponse> stopRide(@PathVariable @Min(1) Long id) {
        // Here would be a call to the rideService.stop(id);
        // Mock response
        RideResponse response = createDummyRideResponse(id, 10L, 123L, RideStatus.FINISHED);
        return ResponseEntity.ok(response);
    }

    // Nakon što korisnik izabere putanju, šalje zahtev + dodatne stavke (tip vozila/bebe/ljubimci)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @GetMapping
    public ResponseEntity<Page<RideResponse>> getRidesHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long passengerId) {

        List<RideResponse> rides = new ArrayList<>();
        rides.add(createDummyRideResponse(101L, 1L, 2L, RideStatus.FINISHED));
        rides.add(createDummyRideResponse(102L, 2L, 3L, RideStatus.REJECTED));
        rides.add(createDummyRideResponse(103L, 1L, 4L, RideStatus.PANIC));

        Pageable pageable = PageRequest.of(page, size);
        Page<RideResponse> ridePage = new PageImpl<>(rides, pageable, rides.size());

        return ResponseEntity.ok(ridePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getRide(@PathVariable @Min(1) Long id) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        return ResponseEntity.ok(createDummyRideResponse(id, 10L, 123L, RideStatus.ACTIVE));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<RideResponse> endRide(@PathVariable @Min(1) Long id, @RequestBody RideEndRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        RideResponse response = createDummyRideResponse(id, 10L, 123L, RideStatus.FINISHED);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/inconsistencies")
    public ResponseEntity<Map<String, String>> reportInconsistency(@PathVariable @Min(1) Long id, @RequestBody InconsistencyRequest request) {
        if (id == 404) throw new ResourceNotFoundException("Ride not found");
        return ResponseEntity.status(201).body(Collections.singletonMap("message", "Inconsistency reported: " + request.getRemark()));
    }

    // 2.6.6 Panic button
    @PutMapping("/{id}/panic")
    public ResponseEntity<RideResponse> panicRide(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody PanicRequest panicRequest) {

        // Mock response
        RideResponse response = new RideResponse();
        response.setId(id);
        response.setPanicPressed(true);
        response.setRejectionReason(panicRequest.getReason());
        response.setRideStatus("PANIC");
        return ResponseEntity.ok(response);
    }

    // dodaje ulinkovanje putnike na voznju, U BATCHU, NE POJEDINACNO
    @PostMapping(value = "/{rideId}/passengers", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddRidePassengers> addPassengerToRide(
            @PathVariable @Min(1) Long rideId,
            @Valid @RequestBody AddRidePassengers passengers) {

        //Mock data - kasnije ide servis
        return ResponseEntity.ok(passengers);
    }

    @PutMapping("/active/start")
    public ResponseEntity<RideResponse> startCurrentRide(/*Authentication auth*/) {
        //String driverEmail = auth.getName(); // from JWT
        //RideResponse started = driverRideService.startCurrentRide(driverEmail);
        // Mocked response
        // treba da updatuje status voznje na "IN_PROGRESS" i da postavi vreme pocetka voznje na trenutno vreme, vrati response voznje
        RideResponse started = new RideResponse();
        return ResponseEntity.ok(started);
    }

    @PostMapping("/favorites/{favouriteRideId}")
    public ResponseEntity<RideResponse> orderFromFavouriteRide(
            @PathVariable @Min(1) Long favouriteRideId
            /*, Authentication auth*/
    ){
        //U sustini, ja dobijem id omiljenje voznje, i auth. Pogledam u bazi da li je taj
        //user vlasnik te omiljene voznje, ako jeste, kreiram novu voznju sa podacima iz omiljene voznje
        //i vracam je kao odgovor. Ovde je sve to mockovano.
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

    @GetMapping("/active")
    public ResponseEntity<RideResponse> getActiveRide(
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long passengerId) {

        if (driverId == null && passengerId == null) {
            throw new IllegalArgumentException("Either driverId or passengerId must be provided");
        }

        // Mock response
        return ResponseEntity.ok(createDummyRideResponse(1L, driverId != null ? driverId : 10L, passengerId != null ? passengerId : 123L, RideStatus.IN_PROGRESS));
    }

    private RideResponse createDummyRideResponse(Long id, Long driverId, Long passengerId, RideStatus status) {
        LocationResponse loc = new LocationResponse("Bulevar oslobodjenja 10", 45.2464, 19.8517);
        RoutePointResponse p1 = new RoutePointResponse(45.2464, 19.8517, 1);
        RoutePointResponse p2 = new RoutePointResponse(45.2464, 19.8520, 2);

        RideResponse response = new RideResponse();
        response.setId(id);
        response.setStatus(status);
        response.setDriverId(driverId);
        response.setPassengerIds(List.of(passengerId));
        response.setStartTime(LocalDateTime.now().minusMinutes(10));
        response.setEndTime(status == RideStatus.FINISHED ? LocalDateTime.now() : null);
        response.setTotalCost(500.0);
        response.setEstimatedCost(450.0);
        response.setVehicleType(VehicleType.STANDARD);
        response.setVehicleLocation(loc);
        response.setRoutePoints(List.of(p1, p2));
        response.setEtaMinutes(5);
        response.setDistanceKm(3.5);
        response.setInconsistencyReports(new ArrayList<>());

        // Set new fields
        response.setDriverEmail("driver" + driverId + "@example.com");
        response.setPassengerEmail("passenger" + passengerId + "@example.com");
        response.setEstimatedTimeInMinutes(5);
        response.setRideStatus(status.name());
        response.setRejectionReason(null);
        response.setPanicPressed(status == RideStatus.PANIC);
        response.setDeparture("Bulevar oslobodjenja 10");
        response.setDestination("Bulevar oslobodjenja 10");

        return response;
    }
}

