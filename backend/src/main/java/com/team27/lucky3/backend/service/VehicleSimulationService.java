package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.entity.Location;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.Vehicle;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.VehicleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class VehicleSimulationService {

    private final VehicleRepository vehicleRepository;
    private final RideRepository rideRepository;
    private final VehicleService vehicleService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    // Per-vehicle patrol state: ordered list of route coordinates
    private final ConcurrentHashMap<Long, List<double[]>> patrolRoutes = new ConcurrentHashMap<>();
    // Current index on the patrol route
    private final ConcurrentHashMap<Long, Integer> patrolIndexes = new ConcurrentHashMap<>();
    // Track which vehicles have a pending OSRM request (avoid spamming)
    private final Set<Long> pendingRouteRequests = ConcurrentHashMap.newKeySet();

    // Per-vehicle simulation lock: vehicleId -> sessionId
    // When set, backend patrol is paused for that vehicle (frontend is driving it)
    private final ConcurrentHashMap<Long, String> simulationLocks = new ConcurrentHashMap<>();
    // Lock expiry timestamps
    private final ConcurrentHashMap<Long, Long> lockExpiry = new ConcurrentHashMap<>();
    private static final long LOCK_TTL_MS = 15_000; // 15 seconds — must be refreshed

    // Novi Sad bounding box for random patrol destination generation
    private static final double NS_LAT_MIN = 45.225;
    private static final double NS_LAT_MAX = 45.280;
    private static final double NS_LNG_MIN = 19.790;
    private static final double NS_LNG_MAX = 19.880;

    private static final double MOVE_MIN_METERS = 40.0;
    private static final double MOVE_MAX_METERS = 60.0;
    private static final double MOVE_PROBABILITY = 0.90;

    private static final String OSRM_BASE = "http://router.project-osrm.org/route/v1/driving/";

    public VehicleSimulationService(
            VehicleRepository vehicleRepository,
            RideRepository rideRepository,
            @org.springframework.context.annotation.Lazy VehicleService vehicleService) {
        this.vehicleRepository = vehicleRepository;
        this.rideRepository = rideRepository;
        this.vehicleService = vehicleService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ── Simulation lock API (called from controller) ────────────────────


    public boolean acquireLock(Long vehicleId, String sessionId) {
        cleanExpiredLocks();
        String existing = simulationLocks.get(vehicleId);
        if (existing == null || existing.equals(sessionId)) {
            simulationLocks.put(vehicleId, sessionId);
            lockExpiry.put(vehicleId, System.currentTimeMillis() + LOCK_TTL_MS);
            return true;
        }
        // Someone else holds the lock and it hasn't expired
        return false;
    }


    public void releaseLock(Long vehicleId, String sessionId) {
        String existing = simulationLocks.get(vehicleId);
        if (existing != null && existing.equals(sessionId)) {
            simulationLocks.remove(vehicleId);
            lockExpiry.remove(vehicleId);
        }
    }

    // Check if frontend is locked
    public boolean isLocked(Long vehicleId) {
        cleanExpiredLocks();
        return simulationLocks.containsKey(vehicleId);
    }

    private void cleanExpiredLocks() {
        long now = System.currentTimeMillis();
        lockExpiry.forEach((vid, expiry) -> {
            if (now > expiry) {
                simulationLocks.remove(vid);
                lockExpiry.remove(vid);
            }
        });
    }

    // ── Scheduled patrol simulation ─────────────────────────────────────

    @Scheduled(fixedRate = 2000)
    public void simulateIdleVehicles() {
        List<Vehicle> activeVehicles = vehicleRepository.findAllActiveVehicles();
        if (activeVehicles.isEmpty()) return;

        // Collect driver IDs that have an active ride (IN_PROGRESS or ACTIVE)
        List<Ride> activeRides = rideRepository.findAllInProgressRides();
        Set<Long> driversWithActiveRide = new HashSet<>();
        for (Ride ride : activeRides) {
            if (ride.getDriver() != null) {
                driversWithActiveRide.add(ride.getDriver().getId());
            }
        }

        // Also consider PENDING/ACCEPTED/SCHEDULED rides — these drivers
        // should also be excluded from patrol simulation (use existing query)
        List<RideStatus> pendingStatuses = Arrays.asList(
                RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.SCHEDULED);
        for (Vehicle v : activeVehicles) {
            if (v.getDriver() == null) continue;
            Long driverId = v.getDriver().getId();
            List<Ride> pending = rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(
                    driverId, pendingStatuses);
            if (!pending.isEmpty()) {
                driversWithActiveRide.add(driverId);
            }
        }

        for (Vehicle vehicle : activeVehicles) {
            if (vehicle.getDriver() == null) continue;
            Long driverId = vehicle.getDriver().getId();
            Long vehicleId = vehicle.getId();

            // Skip vehicles that have an active/pending ride
            if (driversWithActiveRide.contains(driverId)) {
                // Clean up patrol state when a ride starts
                patrolRoutes.remove(vehicleId);
                patrolIndexes.remove(vehicleId);
                continue;
            }

            // Skip vehicles that are locked by a frontend session
            if (isLocked(vehicleId)) continue;

            // 90% chance to move
            if (random.nextDouble() > MOVE_PROBABILITY) continue;

            try {
                advancePatrol(vehicle);
            } catch (Exception e) {
                log.debug("Error advancing patrol for vehicle {}: {}", vehicleId, e.getMessage());
            }
        }
    }

    private void advancePatrol(Vehicle vehicle) {
        Long vehicleId = vehicle.getId();
        List<double[]> route = patrolRoutes.get(vehicleId);

        // Need a new patrol route?
        if (route == null || route.isEmpty()) {
            generatePatrolRoute(vehicle);
            return;
        }

        int currentIdx = patrolIndexes.getOrDefault(vehicleId, 0);
        if (currentIdx >= route.size() - 1) {
            // Completed this patrol loop — generate a new one
            patrolRoutes.remove(vehicleId);
            patrolIndexes.remove(vehicleId);
            generatePatrolRoute(vehicle);
            return;
        }

        // Walk along the route by 40-60 meters
        double metersToMove = MOVE_MIN_METERS + random.nextDouble() * (MOVE_MAX_METERS - MOVE_MIN_METERS);
        double movedMeters = 0;
        double[] currentPos = route.get(currentIdx);

        int idx = currentIdx;
        while (idx < route.size() - 1 && movedMeters < metersToMove) {
            double[] next = route.get(idx + 1);
            double segmentMeters = haversineMeters(currentPos[0], currentPos[1], next[0], next[1]);
            double remaining = metersToMove - movedMeters;

            if (segmentMeters <= remaining) {
                // Move to the next waypoint entirely
                movedMeters += segmentMeters;
                currentPos = next;
                idx++;
            } else {
                // Interpolate within this segment
                double fraction = remaining / segmentMeters;
                double lat = currentPos[0] + fraction * (next[0] - currentPos[0]);
                double lng = currentPos[1] + fraction * (next[1] - currentPos[1]);
                currentPos = new double[]{lat, lng};
                movedMeters += remaining;
                break;
            }
        }

        patrolIndexes.put(vehicleId, idx);
        // Also update the route point at `idx` to the interpolated position
        // so the next tick starts from the exact spot
        if (idx < route.size()) {
            route.set(idx, currentPos);
        }

        // Update the vehicle location
        LocationDto dto = new LocationDto("Patrol", currentPos[0], currentPos[1]);
        vehicleService.updateVehicleLocation(vehicleId, dto);
    }

    private void generatePatrolRoute(Vehicle vehicle) {
        Long vehicleId = vehicle.getId();
        if (pendingRouteRequests.contains(vehicleId)) return;
        pendingRouteRequests.add(vehicleId);

        try {
            Location currentLoc = vehicle.getCurrentLocation();
            double startLat, startLng;
            if (currentLoc != null && currentLoc.getLatitude() != 0) {
                startLat = currentLoc.getLatitude();
                startLng = currentLoc.getLongitude();
            } else {
                // Default to random Novi Sad location
                startLat = NS_LAT_MIN + random.nextDouble() * (NS_LAT_MAX - NS_LAT_MIN);
                startLng = NS_LNG_MIN + random.nextDouble() * (NS_LNG_MAX - NS_LNG_MIN);
            }

            // Generate a random destination 0.5-2km away within Novi Sad
            double destLat, destLng;
            int attempts = 0;
            do {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distKm = 0.5 + random.nextDouble() * 1.5; // 0.5-2km
                destLat = startLat + (distKm / 111.32) * Math.cos(angle);
                destLng = startLng + (distKm / (111.32 * Math.cos(Math.toRadians(startLat)))) * Math.sin(angle);
                attempts++;
            } while (attempts < 5 && (destLat < NS_LAT_MIN || destLat > NS_LAT_MAX
                    || destLng < NS_LNG_MIN || destLng > NS_LNG_MAX));

            // Clamp to bounds
            destLat = Math.max(NS_LAT_MIN, Math.min(NS_LAT_MAX, destLat));
            destLng = Math.max(NS_LNG_MIN, Math.min(NS_LNG_MAX, destLng));

            // Call OSRM for a street-following route
            String coords = startLng + "," + startLat + ";" + destLng + "," + destLat;
            String url = OSRM_BASE + coords + "?overview=full&geometries=geojson";

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return;

            JsonNode root = objectMapper.readTree(response);
            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) return;

            JsonNode geometry = routes.get(0).path("geometry").path("coordinates");
            if (!geometry.isArray() || geometry.isEmpty()) return;

            List<double[]> routePoints = new ArrayList<>();
            for (JsonNode coord : geometry) {
                double lng = coord.get(0).asDouble();
                double lat = coord.get(1).asDouble();
                routePoints.add(new double[]{lat, lng});
            }

            if (routePoints.size() >= 2) {
                patrolRoutes.put(vehicleId, routePoints);
                patrolIndexes.put(vehicleId, 0);
                log.debug("Generated patrol route for vehicle {} with {} points", vehicleId, routePoints.size());
            }
        } catch (Exception e) {
            log.debug("Failed to generate patrol route for vehicle {}: {}", vehicleId, e.getMessage());
        } finally {
            pendingRouteRequests.remove(vehicleId);
        }
    }

    // ── Utilities ───────────────────────────────────────────────────────

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
