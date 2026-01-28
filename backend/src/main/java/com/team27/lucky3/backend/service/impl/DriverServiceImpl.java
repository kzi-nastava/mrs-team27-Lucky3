package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.request.CreateDriverRequest;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.dto.response.DriverResponse;
import com.team27.lucky3.backend.dto.response.DriverStatsResponse;
import com.team27.lucky3.backend.dto.response.DriverStatusResponse;
import com.team27.lucky3.backend.entity.*;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.entity.enums.VehicleStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.team27.lucky3.backend.exception.EmailAlreadyUsedException;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.ActivationTokenRepository;
import com.team27.lucky3.backend.repository.DriverActivitySessionRepository;
import com.team27.lucky3.backend.repository.ReviewRepository;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.repository.VehicleRepository;
import com.team27.lucky3.backend.service.DriverService;
import com.team27.lucky3.backend.service.EmailService;
import com.team27.lucky3.backend.service.ImageService;
import com.team27.lucky3.backend.util.DummyData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final ReviewRepository reviewRepository;
    private final VehicleRepository vehicleRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final DriverActivitySessionRepository activitySessionRepository;
    private final EmailService emailService;
    private final ImageService imageService;

    private final String activationBaseUrl = "http://localhost:4200/driver/set-password?token=";

    @Override
    @Transactional
    public User toggleActivity(Long driverId, boolean targetStatus) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (targetStatus) {
            // Turning ON
            // Spec 2.5/2.2.1: Check working hours before enabling
            if (hasExceededWorkingHours(driverId)) {
                throw new IllegalStateException("Cannot go active: 8-hour working limit exceeded.");
            }
            driver.setActive(true);
            driver.setInactiveRequested(false);
            
            // Start a new activity session
            DriverActivitySession session = new DriverActivitySession();
            session.setDriver(driver);
            session.setStartTime(LocalDateTime.now());
            activitySessionRepository.save(session);
        } else {
            // Turning OFF - check for any rides that prevent going offline
            boolean hasActiveRide = rideRepository.existsByDriverIdAndStatusIn(
                    driverId, List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS));
            boolean hasUpcomingRides = rideRepository.existsByDriverIdAndStatusIn(
                    driverId, List.of(RideStatus.SCHEDULED, RideStatus.PENDING));

            if (hasActiveRide) {
                // Has active ride - set inactiveRequested so driver goes offline after ALL rides complete
                driver.setInactiveRequested(true);
                // Note: checkAndHandleInactiveRequest in RideServiceImpl will check for upcoming rides
                // when the ride ends and keep driver online if there are more rides
            } else if (hasUpcomingRides) {
                // No active ride but has scheduled/pending rides - cannot go offline
                throw new IllegalStateException("Cannot go offline: You have scheduled rides. Complete or cancel them first.");
            } else {
                // No rides at all - can go offline
                driver.setActive(false);
                driver.setInactiveRequested(false);
                
                // End the current activity session
                activitySessionRepository.findByDriverIdAndEndTimeIsNull(driverId)
                        .ifPresent(session -> {
                            session.setEndTime(LocalDateTime.now());
                            activitySessionRepository.save(session);
                        });
            }
        }
        return userRepository.save(driver);
    }

    @Override
    public boolean hasExceededWorkingHours(Long driverId) {
        // Spec 2.5: Driver becomes unavailable if working hours > 8h in the day
        // Calculate from activity sessions (time driver was active/online)
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<DriverActivitySession> sessions = activitySessionRepository.findSessionsSince(driverId, since);

        long totalSeconds = 0;
        LocalDateTime now = LocalDateTime.now();
        for (DriverActivitySession session : sessions) {
            LocalDateTime effectiveStart = session.getStartTime().isBefore(since) ? since : session.getStartTime();
            LocalDateTime effectiveEnd = session.getEndTime() != null ? session.getEndTime() : now;
            totalSeconds += Duration.between(effectiveStart, effectiveEnd).getSeconds();
        }

        // 8 hours = 8 * 60 * 60 = 28800 seconds
        return totalSeconds > 28800;
    }

    @Override
    public boolean hasActiveRide(Long driverId) {
        return rideRepository.existsByDriverIdAndStatusIn(
                driverId, List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS));
    }

    @Override
    public DriverStatusResponse getDriverStatus(Long driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        boolean hasActiveRide = rideRepository.existsByDriverIdAndStatusIn(
                driverId, List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS));
        
        boolean hasUpcomingRides = rideRepository.existsByDriverIdAndStatusIn(
                driverId, List.of(RideStatus.SCHEDULED, RideStatus.PENDING));

        // Calculate working hours in last 24h
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<DriverActivitySession> sessions = activitySessionRepository.findSessionsSince(driverId, since);
        
        long totalSeconds = 0;
        LocalDateTime now = LocalDateTime.now();
        for (DriverActivitySession session : sessions) {
            LocalDateTime effectiveStart = session.getStartTime().isBefore(since) ? since : session.getStartTime();
            LocalDateTime effectiveEnd = session.getEndTime() != null ? session.getEndTime() : now;
            totalSeconds += Duration.between(effectiveStart, effectiveEnd).getSeconds();
        }
        
        boolean workingHoursExceeded = totalSeconds > 28800; // 8 hours
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        String workedHoursToday = hours + "h " + minutes + "m";
        
        // Build status message
        String statusMessage = null;
        if (driver.isInactiveRequested() && hasUpcomingRides) {
            statusMessage = "You cannot go offline because you have scheduled rides. Complete or cancel them first.";
        } else if (driver.isInactiveRequested() && hasActiveRide) {
            statusMessage = "You will go offline after your current ride is complete.";
        }

        return new DriverStatusResponse(
                driver.getId(),
                driver.isActive(),
                driver.isInactiveRequested(),
                hasActiveRide,
                hasUpcomingRides,
                workingHoursExceeded,
                workedHoursToday,
                statusMessage
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DriverStatsResponse getDriverStats(Long driverId) {
        // Verify driver exists
        userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        // Get total earnings from completed rides
        Double totalEarnings = rideRepository.sumTotalEarningsByDriverId(driverId);
        if (totalEarnings == null) {
            totalEarnings = 0.0;
        }

        // Get completed rides count
        Integer completedRides = rideRepository.countCompletedRidesByDriverId(driverId);
        if (completedRides == null) {
            completedRides = 0;
        }

        // Get average rating and count
        Double averageRating = reviewRepository.findAverageDriverRatingByDriverId(driverId);
        Integer totalRatings = reviewRepository.countRatingsByDriverId(driverId);
        if (averageRating == null) {
            averageRating = 0.0;
        }
        if (totalRatings == null) {
            totalRatings = 0;
        }

        // Calculate online hours today from activity sessions (last 24h)
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<DriverActivitySession> sessions = activitySessionRepository.findSessionsSince(driverId, since);
        
        long totalSeconds = 0;
        LocalDateTime now = LocalDateTime.now();
        for (DriverActivitySession session : sessions) {
            // Use the later of session start or 24h ago as the effective start
            LocalDateTime effectiveStart = session.getStartTime().isBefore(since) ? since : session.getStartTime();
            // Use endTime if session ended, otherwise use current time (still active)
            LocalDateTime effectiveEnd = session.getEndTime() != null ? session.getEndTime() : now;
            
            totalSeconds += Duration.between(effectiveStart, effectiveEnd).getSeconds();
        }
        
        // Format as "Xh Ym"
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        String onlineHoursToday = String.format("%dh %dm", hours, minutes);

        return new DriverStatsResponse(
                driverId,
                totalEarnings,
                completedRides,
                averageRating,
                totalRatings,
                onlineHoursToday
        );
    }

    // 2.2.3 Admin creates driver accounts + vehicle info + password setup via email link (admin, driver)
    public DriverResponse createDriver(CreateDriverRequest request, MultipartFile file) throws IOException{
        userRepository.findByEmail(request.getEmail())
                .ifPresent(u -> {
                    throw new EmailAlreadyUsedException("Email is already in use");
                });
        // 1) Create driver (User)
        User driver = new User();
        driver.setName(request.getName());
        driver.setSurname(request.getSurname());
        driver.setEmail(request.getEmail());
        driver.setAddress(request.getAddress());
        driver.setPhoneNumber(request.getPhone());
        driver.setRole(UserRole.DRIVER);          // or string "DRIVER"
        driver.setEnabled(false);                 // cannot login yet
        driver.setActive(false);
        driver.setBlocked(false);
        driver.setInactiveRequested(false);
        driver.setPassword(null);                 // set after activation
        driver.setLastPasswordResetDate(null);
        driver.setProfileImage(null);

        if (file != null && !file.isEmpty()) {

            Image newImage = imageService.store(file);
            driver.setProfileImage(newImage);
        }

        User savedDriver = userRepository.save(driver);

        // 2) Create vehicle
        VehicleInformation vReq = request.getVehicle();
        Vehicle vehicle = new Vehicle();
        vehicle.setModel(vReq.getModel());
        vehicle.setVehicleType(vReq.getVehicleType()); // or enum/string
        vehicle.setLicensePlates(vReq.getLicenseNumber());
        vehicle.setSeatCount(vReq.getPassengerSeats());
        vehicle.setBabyTransport(vReq.getBabyTransport());
        vehicle.setPetTransport(vReq.getPetTransport());
        vehicle.setDriver(savedDriver);

        // NEW: Assign random Novi Sad location to vehicle/driver
        Random random = new SecureRandom();
        Location[] noviSadLocations = {
                new Location("Maksima Gorkog 5, Novi Sad", 45.24947, 19.84440),
                new Location("Cika Stevina 10, Novi Sad", 45.26203, 19.81658),
                new Location("Trg Slobode, Novi Sad", 45.25167, 19.83694),
                new Location("Dunavski Park, Novi Sad", 45.26714, 19.83355),
                new Location("SPENS Sports Center", 45.24000, 19.85000),
                new Location("Srpsko Narodno Pozoriste", 45.25500, 19.83000 ),
                new Location("Novi Sad Airport area", 45.27483, 19.83262 ),
                new Location("Kovilj area", 45.23000, 19.82000 ),
                new Location("Bulevar oslobodjenja", 45.26000, 19.84000),
                new Location("Petrovaradin Fortress nearby", 45.24500, 19.86000)
        };
        int randomIndex = random.nextInt(noviSadLocations.length);
        Location randomLoc = noviSadLocations[randomIndex];
        // Assign to vehicle (if Vehicle has lat/lng/address fields)
        vehicle.setCurrentLocation(randomLoc);
        vehicle.setStatus(VehicleStatus.FREE);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // 3) Create activation token valid 24h
        ActivationToken token = new ActivationToken();
        token.setUser(savedDriver);
        token.setToken(java.util.UUID.randomUUID().toString());
        token.setExpiryDate(LocalDateTime.now().plusHours(24));
        //token.setUsed(false); not needed, default false
        activationTokenRepository.save(token);    // separate table[web:6][web:63][web:15]

        // 4) Send email with one-time link (no password in mail)
        String activationLink = activationBaseUrl + token.getToken();
        String subject = "Driver account activation";
        String body = """
                Your driver account has been created by the administrator.
                Click the link below to set your password (valid for 24 hours):
                %s
                """.formatted(activationLink);

        emailService.sendSimpleMessage(savedDriver.getEmail(), subject, body); //[web:6][web:63][web:66]

        // 5) Map to DriverResponse
        VehicleInformation vehicleInfo = new VehicleInformation(
                savedVehicle.getModel(),
                savedVehicle.getVehicleType(),
                savedVehicle.getLicensePlates(),
                savedVehicle.getSeatCount(),
                savedVehicle.isBabyTransport(),
                savedVehicle.isPetTransport(),
                savedVehicle.getId()
                );

        // active24h: for example, "0h/8h used" on creation
        String active24h = "0h/8h";

        return new DriverResponse(
                savedDriver.getId(),
                savedDriver.getName(),
                savedDriver.getSurname(),
                savedDriver.getEmail(),
                "/api/users/"+savedDriver.getId()+"/profile-image",
                savedDriver.getRole(),
                savedDriver.getPhoneNumber(),
                savedDriver.getAddress(),
                vehicleInfo,
                savedDriver.isActive(),
                savedDriver.isBlocked(),
                active24h
        );
    }

    @Transactional(readOnly = true)
    public DriverResponse getDriver(Long id) {
        User driver = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        Vehicle vehicle = vehicleRepository.findByDriverId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found for driver"));

        VehicleInformation vehicleInfo = new VehicleInformation();
        vehicleInfo.setDriverId(vehicle.getDriver().getId());
        vehicleInfo.setModel(vehicle.getModel());
        vehicleInfo.setVehicleType(vehicle.getVehicleType()); // ensure enum value matches DB
        vehicleInfo.setLicenseNumber(vehicle.getLicensePlates());
        vehicleInfo.setPassengerSeats(vehicle.getSeatCount());
        vehicleInfo.setBabyTransport(vehicle.isBabyTransport());
        vehicleInfo.setPetTransport(vehicle.isPetTransport());
        vehicleInfo.setDriverId(driver.getId());

        String active24h = "0h/8h";

        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getSurname(),
                driver.getEmail(),
                "/api/users/"+driver.getId()+"/profile-image",
                driver.getRole(),
                driver.getPhoneNumber(),
                driver.getAddress(),
                vehicleInfo,
                driver.isActive(),
                driver.isBlocked(),
                active24h
        );
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers() {
        List<User> drivers = userRepository.findAllByRole(UserRole.DRIVER);

        return drivers.stream().map(driver -> {
            Vehicle vehicle = vehicleRepository.findByDriverId(driver.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found for driver"));

            VehicleInformation vehicleInfo = new VehicleInformation();
            vehicleInfo.setDriverId(vehicle.getDriver().getId());
            vehicleInfo.setModel(vehicle.getModel());
            vehicleInfo.setVehicleType(vehicle.getVehicleType()); // ensure enum value matches DB
            vehicleInfo.setLicenseNumber(vehicle.getLicensePlates());
            vehicleInfo.setPassengerSeats(vehicle.getSeatCount());
            vehicleInfo.setBabyTransport(vehicle.isBabyTransport());
            vehicleInfo.setPetTransport(vehicle.isPetTransport());
            vehicleInfo.setDriverId(driver.getId());

            String active24h = "0h/8h";
/*Long id, String name, String surname, String email, String profilePictureUrl,
 UserRole role, String phoneNumber, String address, VehicleInformation vehicle, boolean isActive, boolean isBlocked, String active24h*/
            return new DriverResponse(
                    driver.getId(),
                    driver.getName(),
                    driver.getSurname(),
                    driver.getEmail(),
                    "/api/users/"+driver.getId()+"/profile-image",
                    UserRole.DRIVER,
                    driver.getPhoneNumber(),
                    driver.getAddress(),
                    vehicleInfo,
                    driver.isActive(),
                    driver.isBlocked(),
                    active24h
            );
        }).toList();
    }

    @Override
    public void activateDriverWithPassword(String token, String rawPassword, PasswordEncoder passwordEncoder) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired activation token"));

        if (activationToken.isUsed() || LocalDateTime.now().isAfter(activationToken.getExpiryDate())) {
            throw new IllegalArgumentException("Token expired or already used");
        }

        User driver = activationToken.getUser();
        if (!driver.getRole().equals(UserRole.DRIVER) || driver.isEnabled()) {
            throw new IllegalStateException("Invalid user for activation");
        }

        // Encode and set password
        driver.setPassword(passwordEncoder.encode(rawPassword));
        driver.setLastPasswordResetDate(Timestamp.valueOf(LocalDateTime.now()));
        driver.setEnabled(true);
        driver.setActive(false);
        userRepository.save(driver);

        // Mark token as used and delete
        activationToken.setUsed(true);
        activationTokenRepository.save(activationToken);
        activationTokenRepository.delete(activationToken);  // Cleanup expired/used tokens
    }
}