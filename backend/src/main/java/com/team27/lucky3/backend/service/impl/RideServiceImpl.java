package com.team27.lucky3.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.*;
import com.team27.lucky3.backend.dto.response.*;
import com.team27.lucky3.backend.entity.Location;
import com.team27.lucky3.backend.entity.Panic;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.Vehicle;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.PanicRepository;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.repository.VehicleRepository;
import com.team27.lucky3.backend.repository.InconsistencyReportRepository;
import com.team27.lucky3.backend.entity.InconsistencyReport;
import com.team27.lucky3.backend.service.NotificationService;
import com.team27.lucky3.backend.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;

import static com.team27.lucky3.backend.entity.enums.VehicleType.VAN;

@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final PanicRepository panicRepository;
    private final VehicleRepository vehicleRepository;
    private final InconsistencyReportRepository inconsistencyReportRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double PRICE_PER_KM = 120.0;

    // Base OSRM URL without coordinates
    private static final String OSRM_Base_URL = "http://router.project-osrm.org/route/v1/driving/";

    @Override
    public RideEstimationResponse estimateRide(CreateRideRequest request) {
        LocationDto start = request.getStart();
        LocationDto end = request.getDestination();
        VehicleType type = request.getRequirements() != null ? request.getRequirements().getVehicleType() : VehicleType.STANDARD;

        // Construct coordinates string: start;stop1;stop2;end
        StringBuilder coords = new StringBuilder();
        coords.append(start.getLongitude()).append(",").append(start.getLatitude());

        if (request.getStops() != null) {
            for (LocationDto stop : request.getStops()) {
                coords.append(";").append(stop.getLongitude()).append(",").append(stop.getLatitude());
            }
        }
        coords.append(";").append(end.getLongitude()).append(",").append(end.getLatitude());

        // Fetch Route from OSRM
        String url = OSRM_Base_URL + coords.toString() + "?overview=full&geometries=geojson";

        double distanceKm = 0.0;
        int durationMinutes = 0;
        List<RoutePointResponse> routePoints = new ArrayList<>();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode route = root.path("routes").get(0);

                // OSRM returns distance in meters and duration in seconds
                double distanceMeters = route.path("distance").asDouble();
                double durationSeconds = route.path("duration").asDouble();

                distanceKm = Math.round((distanceMeters / 1000.0) * 100.0) / 100.0;
                durationMinutes = (int) Math.round(durationSeconds / 60.0);

                // Extract Geometry (Coordinates)
                JsonNode coordinates = route.path("geometry").path("coordinates");
                if (coordinates.isArray()) {
                    int order = 0;
                    for (JsonNode coord : coordinates) {
                        // OSRM GeoJSON is [lon, lat]
                        double lon = coord.get(0).asDouble();
                        double lat = coord.get(1).asDouble();
                        // We don't need address for every single point on the line
                        routePoints.add(new RoutePointResponse(new LocationDto("", lat, lon), order++));
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to Haversine if OSRM fails (or handle error appropriately)
            System.err.println("OSRM Routing failed: " + e.getMessage());
            distanceKm = calculateHaversineDistance(mapLocation(start), mapLocation(end));
            // Add stops to fallback distance calculation if needed, but simple for now
            durationMinutes = (int) Math.ceil(distanceKm * 1.2); // Rough estimate
            // Add at least start and end points
            routePoints.add(new RoutePointResponse(start, 0));
            routePoints.add(new RoutePointResponse(end, 1));
        }

        // Calculate Price
        double basePrice = getBasePriceForVehicle(type);
        double estimatedCost = Math.round((basePrice + (distanceKm * PRICE_PER_KM)) * 100.0) / 100.0;

        // Find Closest Driver Time (Simple logic retained)
        int timeToPickup = calculateDriverArrival(start, type);

        return new RideEstimationResponse(durationMinutes, estimatedCost, timeToPickup, distanceKm, routePoints);
    }
    /* staro valjda
    @Override
    public RideEstimationResponse estimateRide(CreateRideRequest request) {
        LocationDto start = request.getStart();
        LocationDto end = request.getDestination();
        VehicleType type = request.getRequirements() != null ? request.getRequirements().getVehicleType() : VehicleType.STANDARD;

        // Fetch Route from OSRM
        String url = String.format(OSRM_API_URL,
                start.getLongitude(), start.getLatitude(),
                end.getLongitude(), end.getLatitude());

        double distanceKm = 0.0;
        int durationMinutes = 0;
        List<RoutePointResponse> routePoints = new ArrayList<>();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode route = root.path("routes").get(0);

                // OSRM returns distance in meters and duration in seconds
                double distanceMeters = route.path("distance").asDouble();
                double durationSeconds = route.path("duration").asDouble();

                distanceKm = Math.round((distanceMeters / 1000.0) * 100.0) / 100.0;
                durationMinutes = (int) Math.round(durationSeconds / 60.0);

                // Extract Geometry (Coordinates)
                JsonNode coordinates = route.path("geometry").path("coordinates");
                if (coordinates.isArray()) {
                    int order = 0;
                    for (JsonNode coord : coordinates) {
                        // OSRM GeoJSON is [lon, lat]
                        double lon = coord.get(0).asDouble();
                        double lat = coord.get(1).asDouble();
                        // We don't need address for every single point on the line
                        routePoints.add(new RoutePointResponse(new LocationDto("", lat, lon), order++));
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to Haversine if OSRM fails (or handle error appropriately)
            System.err.println("OSRM Routing failed: " + e.getMessage());
            distanceKm = calculateHaversineDistance(mapLocation(start), mapLocation(end));
            durationMinutes = (int) Math.ceil(distanceKm * 1.2); // Rough estimate
            // Add at least start and end points
            routePoints.add(new RoutePointResponse(start, 0));
            routePoints.add(new RoutePointResponse(end, 1));
        }

        // Calculate Price
        double basePrice = getBasePriceForVehicle(type);
        double estimatedCost = Math.round((basePrice + (distanceKm * PRICE_PER_KM)) * 100.0) / 100.0;

        // Find Closest Driver Time (Simple logic retained)
        int timeToPickup = calculateDriverArrival(start, type);

        return new RideEstimationResponse(durationMinutes, estimatedCost, timeToPickup, distanceKm, routePoints);
    }
*/
    private int calculateDriverArrival(LocationDto start, VehicleType type) {
        List<Vehicle> activeVehicles = vehicleRepository.findAllActiveVehicles();
        List<Vehicle> candidateVehicles = activeVehicles.stream()
                .filter(v -> v.getVehicleType() == type || type == null)
                .toList();

        if (candidateVehicles.isEmpty()) return -1;

        double minDistance = Double.MAX_VALUE;
        Location startLoc = mapLocation(start);

        for (Vehicle v : candidateVehicles) {
            if (v.getCurrentLocation() != null) {
                double dist = calculateHaversineDistance(startLoc, v.getCurrentLocation());
                if (dist < minDistance) {
                    minDistance = dist;
                }
            }
        }

        return (int) Math.ceil(minDistance / 0.83);
    }

    /*
    1. Ako ne postoji nijedan vozač prijavljen/aktivan, vožnja se odbija i korisniku stiže notifikacija da trenutno nema aktivnih vozača.

    2. Ako su svi vozači trenutno zauzeti i ako imaju već zakazanu buduću vožnju,
    takođe se vožnja odbija uz slanje notifikacije da trenutno nema aktivnih vozača.

    3. Ako ima slobodnih vozača, sistem bira najbližeg, a ako su svi zauzeti, bira se onaj koji je najbliži polazištu
    i najbliži zavšetku trenutne vožnje (preostalo mu je još 10 minuta prethodne vožnje).
    Korisniku se šalje notifikacija o dodeljenoj vožnji.

    4. Ako vozač ima više od 8 radnih sati u poslednja 24 časa, ne postoji mogućnost da mu sistem dodeli vožnju.
     */
    @Override
    @Transactional
    public RideCreated createRide(CreateRideRequest request) {
        User passenger = getCurrentUser();
        RideCreated response = new RideCreated();

        if (passenger == null) {
            throw new IllegalStateException("User must be logged in to create a ride");
        }
        // 1. Look for available drivers (in vehicle repository) - no available drivers
        List<Vehicle> activeVehicles = vehicleRepository.findAllActiveVehicles();
        if (activeVehicles.isEmpty()) { // no available drivers
            response.setStatus(RideStatus.REJECTED);
            response.setRejectionReason("No active drivers available at the moment.");
            return response;
        }
        // estimate ride
        RideEstimationResponse estimation = estimateRide(request);

        // 3. Find available drivers
        LocalDateTime rideStartTime = request.getScheduledTime() != null
                ? request.getScheduledTime()
                : LocalDateTime.now();
        LocalDateTime rideEndTime = rideStartTime.plusMinutes(estimation.getEstimatedTimeInMinutes());

        // Get all driver IDs from active vehicles
        List<Long> activeDriverIds = activeVehicles.stream()
                .map(v -> v.getDriver().getId())
                .collect(Collectors.toList());

        // Find drivers with overlapping rides
        List<Long> busyDriverIds = rideRepository.findDriversWithRidesInTimeRange(
                activeDriverIds,
                rideStartTime,
                rideEndTime
        );

        // Filter out busy drivers
        List<Vehicle> availableFreeVehicles = activeVehicles.stream()
                .filter(v -> !busyDriverIds.contains(v.getDriver().getId()))
                .collect(Collectors.toList());


        // there exists free driver
        if (!availableFreeVehicles.isEmpty()) {
            //assign the closest free driver
            Vehicle assignedVehicle = null;
            double minDistance = Double.MAX_VALUE;
            Location rideStartLocation = mapLocation(request.getStart());
            for (Vehicle v : availableFreeVehicles) {
                if (v.getCurrentLocation() != null) {
                    double dist = calculateHaversineDistance(rideStartLocation, v.getCurrentLocation());
                    if (dist < minDistance) {
                        minDistance = dist;
                        assignedVehicle = v;
                    }
                }
            }
            if (assignedVehicle == null) {
                response.setStatus(RideStatus.REJECTED);
                response.setRejectionReason("No drivers with valid location found.");
                return response;
            }

            // Create and save the ride
            Ride ride = new Ride();
            ride.setStartTime(rideStartTime);
            ride.setEndTime(rideEndTime);
            ride.setScheduledTime(rideStartTime);   //same as start time for now
            ride.setTotalCost(estimation.getEstimatedCost());
            ride.setEstimatedCost(estimation.getEstimatedCost());
            ride.setDistance(estimation.getEstimatedDistance());
            ride.setStatus(RideStatus.SCHEDULED);
            //ride.setPanicPressed(false);
            //ride.setRejectionReason(null);
            ride.setPetTransport(request.getRequirements().isPetTransport());
            ride.setBabyTransport(request.getRequirements().isBabyTransport());
            ride.setRequestedVehicleType(assignedVehicle.getVehicleType());
            ride.setPaid(false); // ne znam sta bih stavio ovde?
            ride.setPassengersExited(false);
            ride.setStartLocation(new Location( request.getStart().getAddress(),
                                                request.getStart().getLatitude(),
                                                request.getStart().getLongitude()));
            ride.setEndLocation(new Location( request.getDestination().getAddress(),
                                              request.getDestination().getLatitude(),
                                              request.getDestination().getLongitude()));
            // Set stops
            if (request.getStops() != null && !request.getStops().isEmpty()) {
                List<Location> rideStops = request.getStops().stream()
                        .map(stop -> new Location(stop.getAddress(),
                                stop.getLatitude(),
                                stop.getLongitude()))
                        .collect(Collectors.toList());
                ride.setStops(rideStops);
            } else {
                ride.setStops(new ArrayList<>());
            }
            ride.setDriver(assignedVehicle.getDriver());
            ride.setPassengers(null);   //TODO: fix this, but for now no need
            ride.setInvitedEmails(request.getPassengerEmails());
            ride.setInconsistencyReports(null);
            ride = rideRepository.save(ride);

            //creating response
            response = RideCreated.fromRide(ride);
            response.setDriverProfilePictureUrl("/api/users/"+assignedVehicle.getDriver().getId()+"/profile-image");
            response.setVehicleModel(assignedVehicle.getModel());
            response.setVehicleLicensePlate(assignedVehicle.getLicensePlates());
            response.setStops(request.getStops());
            response.setRoutePoints(estimation.getRoutePoints());
            response.setEstimatedTimeInMinutes(estimation.getEstimatedTimeInMinutes());

            response.setBabyTransport(request.getRequirements().isBabyTransport());
            response.setPetTransport(request.getRequirements().isPetTransport());
            response.setRequestedVehicleType(assignedVehicle.getVehicleType());

            return response;
        }
        // Further filter based on working hours (max 8h in last 24h)

        //TODO: finish this. It should find driver that is closest to finish his current ride
        response.setStatus(RideStatus.REJECTED);
        response.setRejectionReason("All drivers are currently busy.");
        return response;
    }

    @Override
    @Transactional
    public RideResponse acceptRide(Long id) {
        Ride ride = findById(id);
        User driver = getCurrentUser(); // Driver accepting the ride

        if (ride.getStatus() != RideStatus.PENDING) {
            throw new IllegalStateException("Ride is not pending");
        }

        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
        Ride savedRide = rideRepository.save(ride);
        return mapToResponse(savedRide);
    }

    @Override
    @Transactional
    public RideResponse startRide(Long id) {
        Ride ride = findById(id);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new IllegalStateException("Ride must be accepted before starting");
        }

        ride.setStartTime(LocalDateTime.now());
        ride.setStatus(RideStatus.IN_PROGRESS); // or ACTIVE based on enum
        Ride savedRide = rideRepository.save(ride);
        return mapToResponse(savedRide);
    }

    @Override
    @Transactional
    public RideResponse endRide(Long id, EndRideRequest request) {
        Ride ride = findById(id);

        if (ride.getStatus() != RideStatus.IN_PROGRESS && ride.getStatus() != RideStatus.ACTIVE) {
            throw new IllegalStateException("Ride is not in progress");
        }

        ride.setEndTime(LocalDateTime.now());
        ride.setStatus(RideStatus.FINISHED);
        ride.setPaid(request.getPaid());
        ride.setPassengersExited(request.getPassengersExited());

        Ride savedRide = rideRepository.save(ride);

        // Logic check for next scheduled ride
        if (ride.getDriver() != null) {
            List<Ride> nextRides = rideRepository.findByDriverIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                    ride.getDriver().getId(),
                    RideStatus.SCHEDULED,
                    LocalDateTime.now()
            );

            Vehicle vehicle = vehicleRepository.findByDriverId(ride.getDriver().getId()).orElse(null);
            if (vehicle != null) {
                if (!nextRides.isEmpty()) {
                    vehicle.setStatus(VehicleStatus.BUSY);
                    // "load next ride" logic could mean setting the next ride as active or notifying driver,
                    // but for now we just handle status.
                    // If we wanted to "activate" next ride automatically, we might do it here or let driver do it.
                } else {
                    vehicle.setStatus(VehicleStatus.FREE);
                }
                vehicleRepository.save(vehicle);
            }
        }

        // Trigger notification
        notificationService.sendRideFinishedNotification(savedRide);

        // Time-delayed Inactive Logic
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }

    @Override
    @Transactional
    public RideResponse cancelRide(Long id, String reason) {
        Ride ride = findById(id);
        User currentUser = getCurrentUser();

        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to cancel a ride.");
        }

        // Not cancellable in terminal states
        if (ride.getStatus() == RideStatus.FINISHED
                || ride.getStatus() == RideStatus.CANCELLED
                || ride.getStatus() == RideStatus.PANIC) {
            throw new IllegalStateException("Ride already finished/cancelled");
        }

        boolean isDriver = ride.getDriver() != null && ride.getDriver().getId().equals(currentUser.getId());
        boolean isPassenger = ride.getPassengers() != null && ride.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(currentUser.getId()));

        if (isDriver) {
            // Driver may cancel only before passengers enter the vehicle → before ride starts
            if (ride.getStatus() == RideStatus.IN_PROGRESS || ride.getStatus() == RideStatus.ACTIVE) {
                throw new IllegalStateException("Driver cannot cancel ride after it has started (passengers entered).");
            }

            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalStateException("Driver must provide a reason for cancellation.");
            }

            ride.setRejectionReason(reason.trim());

        } else if (isPassenger) {
            // Passengers can cancel only scheduled rides up to 10 minutes before scheduled start time.
            if (ride.getScheduledTime() == null) {
                throw new IllegalStateException("You can cancel only scheduled rides.");
            }

            LocalDateTime limit = ride.getScheduledTime().minusMinutes(10);
            if (LocalDateTime.now().isAfter(limit)) {
                throw new IllegalStateException("You cannot cancel a scheduled ride within 10 minutes of its start time.");
            }

            if (ride.getStatus() == RideStatus.IN_PROGRESS || ride.getStatus() == RideStatus.ACTIVE) {
                throw new IllegalStateException("Cannot cancel an active ride. Request a stop instead.");
            }

            // Reason isn't mandatory for passenger; store only if provided (non-blank)
            if (reason != null && !reason.trim().isEmpty()) {
                ride.setRejectionReason(reason.trim());
            }
        } else {
            throw new IllegalStateException("User is not authorized to cancel this ride.");
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setEndTime(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        // Time-delayed Inactive Logic
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }

    /* TODO: Check what is better between this and down same function
    @Override
    @Transactional
    public RideResponse panicRide(Long id, RidePanicRequest request) {
        Ride ride = findById(id);
        User user = getCurrentUser();

        ride.setStatus(RideStatus.PANIC);
        ride.setPanicPressed(true);
        ride.setEndTime(LocalDateTime.now());

        Panic panic = new Panic();
        panic.setRide(ride);
        panic.setUser(user);
        panic.setReason(request.getReason());
        panic.setTimestamp(LocalDateTime.now());
        panicRepository.save(panic);

        Ride savedRide = rideRepository.save(ride);

        // Time-delayed Inactive Logic
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }*/

    @Override
    public Ride findById(Long id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + id));
    }

    private void checkAndHandleInactiveRequest(User driver) {
        if (driver != null && driver.isInactiveRequested()) {
            driver.setActive(false);
            driver.setInactiveRequested(false);
            userRepository.save(driver);
        }
    }

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) return null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        // Fallback for tests or if principal is not User object (unlikely in this setup)
        return null;
    }

    private Location mapLocation(LocationDto dto) {
        if (dto == null) return null;
        return new Location(dto.getAddress(), dto.getLatitude(), dto.getLongitude());
    }

    // Manual mapping for now
    private RideResponse mapToResponse(Ride ride) {
        RideResponse res = new RideResponse();
        res.setId(ride.getId());
        res.setStartTime(ride.getStartTime());
        res.setEndTime(ride.getEndTime());
        res.setTotalCost(ride.getTotalCost());
        res.setEstimatedCost(ride.getEstimatedCost());
        res.setDistanceKm(ride.getDistance());
        res.setStatus(ride.getStatus());
        res.setPanicPressed(Boolean.TRUE.equals(ride.getPanicPressed()));
        res.setRejectionReason(ride.getRejectionReason());
        res.setPetTransport(ride.isPetTransport());
        res.setBabyTransport(ride.isBabyTransport());
        res.setVehicleType(ride.getRequestedVehicleType());
        res.setPaid(ride.isPaid());
        res.setPassengersExited(ride.isPassengersExited());

        if (ride.getStartLocation() != null) {
            res.setDeparture(new LocationDto(ride.getStartLocation().getAddress(), ride.getStartLocation().getLatitude(), ride.getStartLocation().getLongitude()));
        }
        if (ride.getEndLocation() != null) {
            res.setDestination(new LocationDto(ride.getEndLocation().getAddress(), ride.getEndLocation().getLatitude(), ride.getEndLocation().getLongitude()));
        }
        res.setScheduledTime(ride.getScheduledTime());

        if (ride.getDriver() != null) {
            // Minimal driver info
            Vehicle vehicle = vehicleRepository.findByDriverId(ride.getDriver().getId()).orElse(null);
            VehicleInformation vInfo = null;
            String model = null;
            String plates = null;
            if(vehicle != null) {
                 vInfo = new VehicleInformation(vehicle.getModel(), vehicle.getVehicleType(), vehicle.getLicensePlates(), vehicle.getSeatCount(), vehicle.isBabyTransport(), vehicle.isPetTransport(), ride.getDriver().getId());
                 model = vehicle.getModel();
                 plates = vehicle.getLicensePlates();
            }
            // Logic to get driver status/time would be complex, simplifying here
            DriverResponse dr = new DriverResponse(ride.getDriver().getId(), ride.getDriver().getName(), ride.getDriver().getSurname(), ride.getDriver().getEmail(), "/api/users/"+ride.getDriver().getId()+"/profile-image", ride.getDriver().getRole(), ride.getDriver().getPhoneNumber(), ride.getDriver().getAddress(), vInfo, ride.getDriver().isActive(), ride.getDriver().isBlocked(), "0h 0m");
            res.setDriver(dr);
            res.setModel(model);
            res.setLicensePlates(plates);
        }

        if (ride.getPassengers() != null) {
             List<UserResponse> passengers = ride.getPassengers().stream().map(p -> new UserResponse(p.getId(), p.getName(), p.getSurname(), p.getEmail(), "/api/users/"+p.getId()+"/profile-image", p.getRole(), p.getPhoneNumber(), p.getAddress())).collect(Collectors.toList());
             res.setPassengers(passengers);
        }

        return res;
    }

    @Override
    @Transactional
    public RideResponse stopRide(Long id, RideStopRequest request) {
        Ride ride = findById(id);
        User currentUser = getCurrentUser();

        // Validate User (Must be driver)
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated.");
        }
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the assigned driver can stop the ride.");
        }

        // Validate Status
        if (ride.getStatus() != RideStatus.IN_PROGRESS && ride.getStatus() != RideStatus.ACTIVE) {
            throw new IllegalStateException("Ride cannot be stopped if it is not in progress.");
        }

        // Update Ride Data
        Location newEndLocation = mapLocation(request.getStopLocation());
        ride.setEndLocation(newEndLocation);
        ride.setEndTime(LocalDateTime.now());
        ride.setStatus(RideStatus.FINISHED);
        ride.setPassengersExited(true);
        ride.setPaid(true);


        double distanceKm = calculateHaversineDistance(
                ride.getStartLocation(),
                newEndLocation
        );
        ride.setDistance(distanceKm);

        double basePrice = getBasePriceForVehicle(ride.getRequestedVehicleType());
        double newPrice = basePrice + (distanceKm * 120.0);
        ride.setTotalCost(Math.round(newPrice * 100.0) / 100.0); // Round to 2 decimals

        Ride savedRide = rideRepository.save(ride);

        // Handle Driver Inactivity
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }

    @Override
    @Transactional
    public RideResponse panicRide(Long id, RidePanicRequest request) {
        Ride ride = findById(id);
        User currentUser = getCurrentUser(); // The user pressing the button

        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated.");
        }

        // Validate that the ride is actually active/in-progress
        if (ride.getStatus() == RideStatus.FINISHED ||
                ride.getStatus() == RideStatus.CANCELLED ||
                ride.getStatus() == RideStatus.PANIC) {
            throw new IllegalStateException("Ride is already finished or cancelled.");
        }

        // Validate that the user is actually part of this ride
        boolean isPassenger = ride.getPassengers().contains(currentUser);
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getId().equals(currentUser.getId());

        if (!isPassenger && !isDriver) {
            throw new IllegalStateException("Only a participant of the ride can activate panic.");
        }

        // Update Ride Status
        ride.setStatus(RideStatus.PANIC);
        ride.setPanicPressed(true);
        ride.setEndTime(LocalDateTime.now());

        // Create Panic Record
        Panic panic = new Panic();
        panic.setRide(ride);
        panic.setUser(currentUser);
        panic.setReason(request.getReason());
        panic.setTimestamp(LocalDateTime.now());
        panicRepository.save(panic);

        // Save Ride
        Ride savedRide = rideRepository.save(ride);

        // Handle Driver Inactivity (If driver pressed it or is affected)
        checkAndHandleInactiveRequest(savedRide.getDriver());

        return mapToResponse(savedRide);
    }

    private double calculateHaversineDistance(Location start, Location end) {
        if (start == null || end == null) return 0.0;
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(end.getLatitude() - start.getLatitude());
        double lonDistance = Math.toRadians(end.getLongitude() - start.getLongitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double getBasePriceForVehicle(VehicleType type) {
        if (type == null) return 120.0;
        return switch (type) {
            case LUXURY -> 200.0;
            case VAN -> 180.0;
            default -> 120.0;
        };
    }

    @Override
    public RideResponse getRideDetails(Long id) {
        return mapToResponse(findById(id));
    }

    @Override
    public Page<RideResponse> getRidesHistory(Pageable pageable, LocalDateTime fromDate, LocalDateTime toDate, Long driverId, Long passengerId, String status) {
        Specification<Ride> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (driverId != null) {
                predicates.add(cb.equal(root.get("driver").get("id"), driverId));
            }
            if (passengerId != null) {
                Join<Ride, User> passengers = root.join("passengers");
                predicates.add(cb.equal(passengers.get("id"), passengerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), RideStatus.valueOf(status)));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endTime"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Ride> ridesPage = rideRepository.findAll(spec, pageable);
        List<RideResponse> dtos = ridesPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, ridesPage.getTotalElements());
    }

    @Override
    @Transactional
    public void reportInconsistency(Long rideId, InconsistencyRequest request) {
        Ride ride = findById(rideId);
        User reporter = getCurrentUser();

        InconsistencyReport report = new InconsistencyReport();
        report.setRide(ride);
        report.setDescription(request.getRemark());
        report.setTimestamp(LocalDateTime.now());
        report.setReporter(reporter);

        inconsistencyReportRepository.save(report);
    }

    @Override
    public RideResponse getActiveRide(Long userId) {
        if (userId == null) {
            User current = getCurrentUser();
            if (current == null) throw new ResourceNotFoundException("User not found");
            userId = current.getId();
        }

        final Long finalUserId = userId;
        Specification<Ride> spec = (root, query, cb) -> {
            Join<Ride, User> driver = root.join("driver", jakarta.persistence.criteria.JoinType.LEFT);
            Join<Ride, User> passengers = root.join("passengers", jakarta.persistence.criteria.JoinType.LEFT);

            Predicate isUser = cb.or(
                    cb.equal(driver.get("id"), finalUserId),
                    cb.equal(passengers.get("id"), finalUserId)
            );

            Predicate isActive = cb.or(
                    cb.equal(root.get("status"), RideStatus.ACTIVE),
                    cb.equal(root.get("status"), RideStatus.IN_PROGRESS),
                    cb.equal(root.get("status"), RideStatus.ACCEPTED)
            );

            return cb.and(isUser, isActive);
        };

        return rideRepository.findAll(spec).stream()
                .findFirst()
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No active ride for user: " + finalUserId));
    }
}
