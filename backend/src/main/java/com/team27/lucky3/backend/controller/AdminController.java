package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.UpdateVehiclePriceRequest;
import com.team27.lucky3.backend.dto.response.AdminStatsResponse;
import com.team27.lucky3.backend.dto.response.VehiclePriceResponse;
import com.team27.lucky3.backend.entity.DriverChangeRequest;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.VehiclePrice;
import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import com.team27.lucky3.backend.repository.ReviewRepository;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.service.DriverChangeRequestService;
import com.team27.lucky3.backend.service.VehiclePriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class AdminController {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final VehiclePriceService vehiclePriceService;

    // 2.14 Get all vehicle prices
    @GetMapping("/vehicle-prices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VehiclePriceResponse>> getAllVehiclePrices() {
        List<VehiclePriceResponse> prices = vehiclePriceService.getAllPrices().stream()
                .map(p -> new VehiclePriceResponse(p.getId(), p.getVehicleType(), p.getBaseFare(), p.getPricePerKm()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(prices);
    }

    // 2.14 Update vehicle price
    @PutMapping("/vehicle-prices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehiclePriceResponse> updateVehiclePrice(@Valid @RequestBody UpdateVehiclePriceRequest request) {
        VehiclePrice updated = vehiclePriceService.updatePrice(
                request.getVehicleType(), request.getBaseFare(), request.getPricePerKm());
        VehiclePriceResponse response = new VehiclePriceResponse(
                updated.getId(), updated.getVehicleType(), updated.getBaseFare(), updated.getPricePerKm());
        return ResponseEntity.ok(response);
    }

    // Admin dashboard statistics
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStatsResponse> getAdminStats() {
        Integer activeRidesCount = rideRepository.countActiveRides();
        Double averageDriverRating = reviewRepository.getOverallAverageDriverRating();
        Integer driversOnlineCount = userRepository.countOnlineDrivers();
        
        // Count total passengers in active rides
        List<Ride> activeRides = rideRepository.findAllActiveRidesForPassengerCount();
        int totalPassengers = activeRides.stream()
                .mapToInt(ride -> ride.getPassengers() != null ? ride.getPassengers().size() : 0)
                .sum();

        AdminStatsResponse stats = new AdminStatsResponse(
                activeRidesCount != null ? activeRidesCount : 0,
                averageDriverRating != null ? Math.round(averageDriverRating * 100.0) / 100.0 : 0.0,
                driversOnlineCount != null ? driversOnlineCount : 0,
                totalPassengers
        );
        
        return ResponseEntity.ok(stats);
    }

}
