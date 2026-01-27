package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.entity.Location;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.Vehicle;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Background service that monitors vehicle locations for active rides
 * and automatically updates the ride cost based on distance traveled.
 * 
 * This runs every 5 seconds and checks all IN_PROGRESS rides.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideCostTrackingService {

    private final RideRepository rideRepository;
    private final VehicleRepository vehicleRepository;

    private static final double PRICE_PER_KM = 120.0;
    private static final double MIN_MOVEMENT_KM = 0.001; // 1 meter minimum movement
    private static final double MAX_MOVEMENT_KM = 2.0; // 2km max per poll (filter GPS jumps)

    /**
     * Scheduled task that runs every 5 seconds to track vehicle movements
     * and update ride costs for all active rides.
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    @Transactional
    public void trackActiveRideCosts() {
        List<Ride> activeRides = rideRepository.findAllInProgressRides();
        
        if (activeRides.isEmpty()) {
            return; // No active rides to track
        }
        
        log.debug("Tracking costs for {} active rides", activeRides.size());
        
        for (Ride ride : activeRides) {
            try {
                updateRideCost(ride);
            } catch (Exception e) {
                log.error("Error updating cost for ride {}: {}", ride.getId(), e.getMessage());
            }
        }
    }

    /**
     * Updates the cost for a single ride based on vehicle movement.
     */
    private void updateRideCost(Ride ride) {
        if (ride.getDriver() == null) {
            return;
        }

        // Get the driver's vehicle
        Vehicle vehicle = vehicleRepository.findByDriverId(ride.getDriver().getId()).orElse(null);
        if (vehicle == null || vehicle.getCurrentLocation() == null) {
            log.debug("No vehicle or location for ride {}", ride.getId());
            return;
        }

        Location currentVehicleLocation = vehicle.getCurrentLocation();
        double currentLat = currentVehicleLocation.getLatitude();
        double currentLon = currentVehicleLocation.getLongitude();

        // Initialize tracking if this is the first check
        if (ride.getLastTrackedLatitude() == null || ride.getLastTrackedLongitude() == null) {
            // First time tracking - use ride start location or current vehicle location
            Location startLoc = ride.getStartLocation();
            if (startLoc != null) {
                ride.setLastTrackedLatitude(startLoc.getLatitude());
                ride.setLastTrackedLongitude(startLoc.getLongitude());
            } else {
                ride.setLastTrackedLatitude(currentLat);
                ride.setLastTrackedLongitude(currentLon);
            }
            
            if (ride.getDistanceTraveled() == null) {
                ride.setDistanceTraveled(0.0);
            }
            
            // Initialize total cost to base price if not set
            if (ride.getTotalCost() == null || ride.getTotalCost() == 0) {
                double basePrice = getBasePriceForVehicle(ride.getRequestedVehicleType());
                ride.setTotalCost(basePrice);
            }
            
            rideRepository.save(ride);
            log.info("Initialized tracking for ride {}: startLat={}, startLon={}", 
                    ride.getId(), ride.getLastTrackedLatitude(), ride.getLastTrackedLongitude());
            return;
        }

        // Calculate distance from last tracked position to current position
        double distanceKm = calculateHaversineDistance(
                ride.getLastTrackedLatitude(), 
                ride.getLastTrackedLongitude(),
                currentLat, 
                currentLon
        );

        // Only update if vehicle has moved a meaningful distance
        if (distanceKm < MIN_MOVEMENT_KM) {
            return; // No significant movement
        }

        // Filter out GPS jumps (unreasonably large movements in 5 seconds)
        if (distanceKm > MAX_MOVEMENT_KM) {
            log.warn("Ride {}: Ignoring large movement of {} km (possible GPS jump)", ride.getId(), distanceKm);
            // Update tracked position anyway to prevent accumulating error
            ride.setLastTrackedLatitude(currentLat);
            ride.setLastTrackedLongitude(currentLon);
            rideRepository.save(ride);
            return;
        }

        // Update distance traveled
        double newDistanceTraveled = (ride.getDistanceTraveled() != null ? ride.getDistanceTraveled() : 0.0) + distanceKm;
        ride.setDistanceTraveled(Math.round(newDistanceTraveled * 1000.0) / 1000.0); // Round to 3 decimal places

        // Calculate new total cost
        double basePrice = getBasePriceForVehicle(ride.getRequestedVehicleType());
        double newTotalCost = basePrice + (ride.getDistanceTraveled() * PRICE_PER_KM);
        ride.setTotalCost(Math.round(newTotalCost * 100.0) / 100.0); // Round to 2 decimal places

        // Update last tracked position
        ride.setLastTrackedLatitude(currentLat);
        ride.setLastTrackedLongitude(currentLon);

        // Also update the distance field (total route distance)
        ride.setDistance(ride.getDistanceTraveled());

        rideRepository.save(ride);

        log.info("Ride {} cost updated: distance={}km (+{}km), totalCost={} RSD", 
                ride.getId(), ride.getDistanceTraveled(), 
                Math.round(distanceKm * 1000.0) / 1000.0, 
                ride.getTotalCost());
    }

    /**
     * Calculate distance between two points using Haversine formula.
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Get base price for vehicle type.
     */
    private double getBasePriceForVehicle(VehicleType type) {
        if (type == null) return 120.0;
        return switch (type) {
            case LUXURY -> 360.0;
            case VAN -> 180.0;
            default -> 120.0;
        };
    }
}
