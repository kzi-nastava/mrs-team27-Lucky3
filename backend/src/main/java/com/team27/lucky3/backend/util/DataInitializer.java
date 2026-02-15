package com.team27.lucky3.backend.util;

import com.team27.lucky3.backend.entity.*;
import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.entity.enums.VehicleStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final RideRepository rideRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRouteRepository favoriteRouteRepository;
    private final DriverActivitySessionRepository driverActivitySessionRepository;
    private final PanicRepository panicRepository;
    private final DriverChangeRequestRepository driverChangeRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("DataInitializer: Checking if data initialization is needed...");
        long count = userRepository.count();
        System.out.println("DataInitializer: Current user count: " + count);

        if (count == 0) {
            System.out.println("DataInitializer: No users found. Initializing dummy data...");
            initializeData();
            System.out.println("DataInitializer: Initialization finished.");
        } else {
            System.out.println("DataInitializer: Users already exist. Skipping initialization.");
            System.out.println("DataInitializer: To force re-initialization, clear the database users table or use ddl-auto=create.");
        }
    }

    private void initializeData() {
        System.out.println("Initializing dummy data...");

        // 1. Create Driver
        User driver = new User();
        driver.setName("John");
        driver.setSurname("Doe");
        driver.setEmail("driver@example.com");
        driver.setPassword(passwordEncoder.encode("password"));
        driver.setRole(UserRole.DRIVER);
        driver.setActive(true);
        driver.setAddress("Bulevar Oslobodjenja 1, Novi Sad");
        driver.setPhoneNumber("555-1234");
        driver.setEnabled(true);
        userRepository.save(driver);

        // 1b. Create Second Driver
        User driver2 = new User();
        driver2.setName("Mike");
        driver2.setSurname("Johnson");
        driver2.setEmail("driver2@example.com");
        driver2.setPassword(passwordEncoder.encode("password"));
        driver2.setRole(UserRole.DRIVER);
        driver2.setActive(false); // Offline (last session ended 1h ago)
        driver2.setAddress("Futoska 15, Novi Sad");
        driver2.setPhoneNumber("555-4321");
        driver2.setEnabled(true);
        userRepository.save(driver2);

        // 2. Create Passenger
        User passenger = new User();
        passenger.setName("Jane");
        passenger.setSurname("Smith");
        passenger.setEmail("passenger@example.com");
        passenger.setPassword(passwordEncoder.encode("password"));
        passenger.setRole(UserRole.PASSENGER);
        passenger.setAddress("Jevrejska 10, Novi Sad");
        passenger.setPhoneNumber("555-5678");
        passenger.setEnabled(true);
        userRepository.save(passenger);

        // 2b. Create Second Passenger
        User passenger2 = new User();
        passenger2.setName("Emily");
        passenger2.setSurname("Davis");
        passenger2.setEmail("passenger2@example.com");
        passenger2.setPassword(passwordEncoder.encode("password"));
        passenger2.setRole(UserRole.PASSENGER);
        passenger2.setAddress("Zmaj Jovina 5, Novi Sad");
        passenger2.setPhoneNumber("555-8765");
        passenger2.setEnabled(true);
        userRepository.save(passenger2);

        // 2c. Create Third Passenger  
        User passenger3 = new User();
        passenger3.setName("Robert");
        passenger3.setSurname("Wilson");
        passenger3.setEmail("passenger3@example.com");
        passenger3.setPassword(passwordEncoder.encode("password"));
        passenger3.setRole(UserRole.PASSENGER);
        passenger3.setAddress("Dunavska 20, Novi Sad");
        passenger3.setPhoneNumber("555-3456");
        passenger3.setEnabled(true);
        userRepository.save(passenger3);
        
        // 3. Create Admin
        User admin = new User();
        admin.setName("Admin");
        admin.setSurname("User");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRole(UserRole.ADMIN);
        admin.setAddress("Trg Slobode 1, Novi Sad");
        admin.setPhoneNumber("555-9999");
        admin.setEnabled(true);
        userRepository.save(admin);

        // 4. Create Vehicle for Driver 1 (BUSY because has active ride)
        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setVehicleType(VehicleType.STANDARD);
        vehicle.setModel("Toyota Camry");
        vehicle.setLicensePlates("XYZ-1234");
        vehicle.setSeatCount(4);
        vehicle.setBabyTransport(true);
        vehicle.setPetTransport(false);
        vehicle.setStatus(VehicleStatus.BUSY); // Has IN_PROGRESS ride
        vehicle.setCurrentPanic(false);
        // Vehicle location near Promenada (start of in-progress ride) but slightly ahead
        vehicle.setCurrentLocation(new Location("Near Univerzitetski Park, Novi Sad", 45.2450, 19.8515));
        vehicleRepository.save(vehicle);

        // 4b. Create Vehicle for Driver 2 (FREE - no active ride)
        Vehicle vehicle2 = new Vehicle();
        vehicle2.setDriver(driver2);
        vehicle2.setVehicleType(VehicleType.VAN);
        vehicle2.setModel("Mercedes Vito");
        vehicle2.setLicensePlates("ABC-5678");
        vehicle2.setSeatCount(7);
        vehicle2.setBabyTransport(true);
        vehicle2.setPetTransport(true);
        vehicle2.setStatus(VehicleStatus.FREE); // Driver 2 is currently offline, so vehicle is FREE (or should it be unavailable? Status only has FREE/BUSY)
        vehicle2.setCurrentPanic(false);
        vehicle2.setCurrentLocation(new Location("Spens, Novi Sad", 45.2465, 19.8480));
        vehicleRepository.save(vehicle2);

        // 5. Create Rides

        // Ride 1: SCHEDULED (later today) - Driver 1
        Ride ridePending = new Ride();
        ridePending.setDriver(driver);
        ridePending.setPassengers(Collections.singleton(passenger));
        ridePending.setStatus(RideStatus.SCHEDULED);
        ridePending.setStartLocation(new Location("Trg Slobode, Novi Sad", 45.2551, 19.8450));
        ridePending.setEndLocation(new Location("Petrovaradinska Tvrdjava, Novi Sad", 45.2530, 19.8610));
        ridePending.setScheduledTime(LocalDateTime.now().plusHours(2));
        ridePending.setStartTime(ridePending.getScheduledTime());
        ridePending.setEndTime(ridePending.getStartTime().plusMinutes(15));
        ridePending.setEstimatedCost(504.00);
        ridePending.setDistance(3.2);
        ridePending.setTotalCost(504.00);
        ridePending.setPetTransport(false);
        ridePending.setBabyTransport(false);
        ridePending.setRequestedVehicleType(VehicleType.STANDARD);
        ridePending.setPanicPressed(false);
        ridePending.setPaid(false);
        ridePending.setPassengersExited(false);
        ridePending.setDistanceTraveled(0.0);
        ridePending.setRateBaseFare(120.0);
        ridePending.setRatePricePerKm(120.0);
        rideRepository.save(ridePending);

        // Ride 2: IN_PROGRESS (Active now) - Driver 1
        // Start location completed (index -1), currently heading to first stop
        Ride rideActive = new Ride();
        rideActive.setDriver(driver);
        rideActive.setPassengers(Collections.singleton(passenger));
        rideActive.setStatus(RideStatus.IN_PROGRESS);
        rideActive.setStartLocation(new Location("Promenada Shopping Center, Novi Sad", 45.2420, 19.8520));
        rideActive.setEndLocation(new Location("Strand Beach, Novi Sad", 45.2429, 19.8424));
        rideActive.setStartTime(LocalDateTime.now().minusMinutes(10));
        rideActive.setEstimatedCost(372.00);
        rideActive.setDistance(2.1);
        rideActive.setTotalCost(372.00);
        rideActive.setPetTransport(false);
        rideActive.setBabyTransport(true);
        rideActive.setRequestedVehicleType(VehicleType.STANDARD);
        rideActive.setPanicPressed(false);
        rideActive.setPaid(false);
        rideActive.setPassengersExited(false);
        rideActive.setRateBaseFare(120.0);
        rideActive.setRatePricePerKm(120.0);
        rideActive.setDistanceTraveled(0.5);
        rideActive.setLastTrackedLatitude(45.2450);
        rideActive.setLastTrackedLongitude(19.8515);
        // Add multiple stops
        rideActive.setStops(List.of(
                new Location("Univerzitetski Park, Novi Sad", 45.2455, 19.8510),
                new Location("Dunavski Park, Novi Sad", 45.2560, 19.8520)
        ));
        // Add route points for the ride
        rideActive.setRoutePoints(List.of(
                new Location("Start", 45.2420, 19.8520),
                new Location("Point1", 45.2440, 19.8515),
                new Location("Point2", 45.2455, 19.8510),
                new Location("Point3", 45.2500, 19.8490),
                new Location("Point4", 45.2560, 19.8520),
                new Location("End", 45.2429, 19.8424)
        ));
        // Mark start location as completed (index -1)
        Set<Integer> completedStops = new HashSet<>();
        completedStops.add(-1); // Start location completed
        rideActive.setCompletedStopIndexes(completedStops);
        rideRepository.save(rideActive);

        // Ride 3: FINISHED (Yesterday) - Driver 1
        Ride rideFinished = new Ride();
        rideFinished.setDriver(driver);
        rideFinished.setPassengers(Collections.singleton(passenger));
        rideFinished.setStatus(RideStatus.FINISHED);
        rideFinished.setStartLocation(new Location("Liman Park, Novi Sad", 45.248, 19.840));
        rideFinished.setEndLocation(new Location("Spens, Novi Sad", 45.2465, 19.8480));
        rideFinished.setStartTime(LocalDateTime.now().minusDays(1).minusHours(1));
        rideFinished.setEndTime(LocalDateTime.now().minusDays(1));
        rideFinished.setTotalCost(300.00);
        rideFinished.setEstimatedCost(300.00);
        rideFinished.setDistance(1.5);
        rideFinished.setRequestedVehicleType(VehicleType.STANDARD);
        rideFinished.setPanicPressed(false);
        rideFinished.setPaid(true);
        rideFinished.setPassengersExited(true);
        rideFinished.setDistanceTraveled(1.5);
        rideFinished.setPetTransport(false);
        rideFinished.setBabyTransport(false);
        rideFinished.setRateBaseFare(120.0);
        rideFinished.setRatePricePerKm(120.0);
        rideRepository.save(rideFinished);

        // Ride 4: FINISHED (2 days ago) - Driver 1 with passenger2
        Ride rideFinished2 = new Ride();
        rideFinished2.setDriver(driver);
        rideFinished2.setPassengers(Collections.singleton(passenger2));
        rideFinished2.setStatus(RideStatus.FINISHED);
        rideFinished2.setStartLocation(new Location("Zeleznicka Stanica, Novi Sad", 45.2670, 19.8330));
        rideFinished2.setEndLocation(new Location("Sajmiste, Novi Sad", 45.2580, 19.8200));
        rideFinished2.setStartTime(LocalDateTime.now().minusDays(2).minusHours(3));
        rideFinished2.setEndTime(LocalDateTime.now().minusDays(2).minusHours(2));
        rideFinished2.setTotalCost(456.00);
        rideFinished2.setEstimatedCost(456.00);
        rideFinished2.setDistance(2.8);
        rideFinished2.setRequestedVehicleType(VehicleType.STANDARD);
        rideFinished2.setPanicPressed(false);
        rideFinished2.setPaid(true);
        rideFinished2.setPassengersExited(true);
        rideFinished2.setDistanceTraveled(2.8);
        rideFinished2.setPetTransport(false);
        rideFinished2.setBabyTransport(false);
        rideFinished2.setRateBaseFare(120.0);
        rideFinished2.setRatePricePerKm(120.0);
        rideRepository.save(rideFinished2);

        // Ride 5: FINISHED (3 days ago) - Driver 2 with passenger
        Ride rideFinished3 = new Ride();
        rideFinished3.setDriver(driver2);
        rideFinished3.setPassengers(Collections.singleton(passenger));
        rideFinished3.setStatus(RideStatus.FINISHED);
        rideFinished3.setStartLocation(new Location("Futog, Novi Sad", 45.2400, 19.7200));
        rideFinished3.setEndLocation(new Location("Centar, Novi Sad", 45.2500, 19.8400));
        rideFinished3.setStartTime(LocalDateTime.now().minusDays(3).minusHours(2));
        rideFinished3.setEndTime(LocalDateTime.now().minusDays(3).minusHours(1));
        rideFinished3.setTotalCost(1200.00);
        rideFinished3.setEstimatedCost(1200.00);
        rideFinished3.setDistance(8.5);
        rideFinished3.setRequestedVehicleType(VehicleType.VAN);
        rideFinished3.setPanicPressed(false);
        rideFinished3.setPaid(true);
        rideFinished3.setPassengersExited(true);
        rideFinished3.setDistanceTraveled(8.5);
        rideFinished3.setPetTransport(true);
        rideFinished3.setBabyTransport(false);
        rideFinished3.setRateBaseFare(180.0);
        rideFinished3.setRatePricePerKm(120.0);
        rideRepository.save(rideFinished3);

        // Ride 6: SCHEDULED (Tomorrow) - Driver 2 with passenger3
        Ride rideScheduled = new Ride();
        rideScheduled.setDriver(driver2);
        rideScheduled.setPassengers(Collections.singleton(passenger3));
        rideScheduled.setStatus(RideStatus.SCHEDULED);
        rideScheduled.setStartLocation(new Location("Aerodrom Nikola Tesla, Belgrade", 44.8184, 20.3091));
        rideScheduled.setEndLocation(new Location("Trg Republike, Novi Sad", 45.2551, 19.8450));
        rideScheduled.setScheduledTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0));
        rideScheduled.setStartTime(rideScheduled.getScheduledTime());
        rideScheduled.setEndTime(rideScheduled.getStartTime().plusHours(1));
        rideScheduled.setEstimatedCost(10380.00);
        rideScheduled.setDistance(85.0);
        rideScheduled.setTotalCost(10380.00);
        rideScheduled.setPetTransport(false);
        rideScheduled.setBabyTransport(false);
        rideScheduled.setRequestedVehicleType(VehicleType.VAN);
        rideScheduled.setPanicPressed(false);
        rideScheduled.setPaid(false);
        rideScheduled.setPassengersExited(false);
        rideScheduled.setDistanceTraveled(0.0);
        rideScheduled.setRateBaseFare(180.0);
        rideScheduled.setRatePricePerKm(120.0);
        rideRepository.save(rideScheduled);

        // Ride 7: CANCELLED_BY_PASSENGER (Yesterday) - Driver 1 with passenger2
        Ride rideCancelled = new Ride();
        rideCancelled.setDriver(driver);
        rideCancelled.setPassengers(Collections.singleton(passenger2));
        rideCancelled.setStatus(RideStatus.CANCELLED_BY_PASSENGER);
        rideCancelled.setStartLocation(new Location("Liman 4, Novi Sad", 45.2350, 19.8250));
        rideCancelled.setEndLocation(new Location("Telep, Novi Sad", 45.2300, 19.8000));
        rideCancelled.setStartTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0));
        rideCancelled.setEndTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(15));
        rideCancelled.setEstimatedCost(480.00);
        rideCancelled.setTotalCost(480.00);
        rideCancelled.setDistance(3.0);
        rideCancelled.setRejectionReason("Driver had vehicle issue");
        rideCancelled.setRequestedVehicleType(VehicleType.STANDARD);
        rideCancelled.setPanicPressed(false);
        rideCancelled.setPaid(false);
        rideCancelled.setPassengersExited(false);
        rideCancelled.setDistanceTraveled(0.0);
        rideCancelled.setPetTransport(false);
        rideCancelled.setBabyTransport(false);
        rideCancelled.setRateBaseFare(120.0);
        rideCancelled.setRatePricePerKm(120.0);
        rideRepository.save(rideCancelled);

        // Ride 8: FINISHED with multiple passengers (4 days ago)
        Set<User> multiplePassengers = new HashSet<>();
        multiplePassengers.add(passenger);
        multiplePassengers.add(passenger2);
        Ride rideMultiPassenger = new Ride();
        rideMultiPassenger.setDriver(driver);
        rideMultiPassenger.setPassengers(multiplePassengers);
        rideMultiPassenger.setStatus(RideStatus.FINISHED);
        rideMultiPassenger.setStartLocation(new Location("Exit Festival, Petrovaradin", 45.2530, 19.8610));
        rideMultiPassenger.setEndLocation(new Location("Centar, Novi Sad", 45.2550, 19.8450));
        rideMultiPassenger.setStartTime(LocalDateTime.now().minusDays(4).withHour(2).withMinute(30));
        rideMultiPassenger.setEndTime(LocalDateTime.now().minusDays(4).withHour(2).withMinute(50));
        rideMultiPassenger.setTotalCost(264.00);
        rideMultiPassenger.setEstimatedCost(264.00);
        rideMultiPassenger.setDistance(1.2);
        rideMultiPassenger.setRequestedVehicleType(VehicleType.STANDARD);
        rideMultiPassenger.setPanicPressed(false);
        rideMultiPassenger.setPaid(true);
        rideMultiPassenger.setPassengersExited(true);
        rideMultiPassenger.setDistanceTraveled(1.2);
        rideMultiPassenger.setPetTransport(false);
        rideMultiPassenger.setBabyTransport(false);
        rideMultiPassenger.setRateBaseFare(120.0);
        rideMultiPassenger.setRatePricePerKm(120.0);
        rideRepository.save(rideMultiPassenger);

        // Ride 9: FINISHED with PANIC (5 days ago) - to show panic history
        Ride ridePanicHistory = new Ride();
        ridePanicHistory.setDriver(driver2);
        ridePanicHistory.setPassengers(Collections.singleton(passenger3));
        ridePanicHistory.setStatus(RideStatus.FINISHED);
        ridePanicHistory.setStartLocation(new Location("Grbavica, Novi Sad", 45.2400, 19.8350));
        ridePanicHistory.setEndLocation(new Location("Detelinara, Novi Sad", 45.2600, 19.8150));
        ridePanicHistory.setStartTime(LocalDateTime.now().minusDays(5).minusHours(4));
        ridePanicHistory.setEndTime(LocalDateTime.now().minusDays(5).minusHours(3));
        ridePanicHistory.setTotalCost(600.00);
        ridePanicHistory.setEstimatedCost(600.00);
        ridePanicHistory.setDistance(3.5);
        ridePanicHistory.setRequestedVehicleType(VehicleType.VAN);
        ridePanicHistory.setPanicPressed(true);
        ridePanicHistory.setPanicReason("Passenger felt unsafe due to erratic driving");
        ridePanicHistory.setPaid(true);
        ridePanicHistory.setPassengersExited(true);
        ridePanicHistory.setDistanceTraveled(3.5);
        ridePanicHistory.setPetTransport(false);
        ridePanicHistory.setBabyTransport(true);
        ridePanicHistory.setRateBaseFare(180.0);
        ridePanicHistory.setRatePricePerKm(120.0);
        rideRepository.save(ridePanicHistory);

        // Ride 10: CANCELLED_BY_DRIVER (6 days ago) - Driver 2 cancelled
        Ride rideCancelledByDriver = new Ride();
        rideCancelledByDriver.setDriver(driver2);
        rideCancelledByDriver.setPassengers(Collections.singleton(passenger));
        rideCancelledByDriver.setStatus(RideStatus.CANCELLED_BY_DRIVER);
        rideCancelledByDriver.setStartLocation(new Location("Bulevar Cara Lazara, Novi Sad", 45.2440, 19.8420));
        rideCancelledByDriver.setEndLocation(new Location("Futoska Pijaca, Novi Sad", 45.2530, 19.8200));
        rideCancelledByDriver.setStartTime(LocalDateTime.now().minusDays(6).minusHours(5));
        rideCancelledByDriver.setEndTime(LocalDateTime.now().minusDays(6).minusHours(5).plusMinutes(5));
        rideCancelledByDriver.setTotalCost(540.00);
        rideCancelledByDriver.setEstimatedCost(540.00);
        rideCancelledByDriver.setDistance(3.5);
        rideCancelledByDriver.setRejectionReason("Vehicle broke down");
        rideCancelledByDriver.setRequestedVehicleType(VehicleType.VAN);
        rideCancelledByDriver.setPanicPressed(false);
        rideCancelledByDriver.setPaid(false);
        rideCancelledByDriver.setPassengersExited(false);
        rideCancelledByDriver.setDistanceTraveled(0.0);
        rideCancelledByDriver.setPetTransport(false);
        rideCancelledByDriver.setBabyTransport(false);
        rideCancelledByDriver.setRateBaseFare(180.0);
        rideCancelledByDriver.setRatePricePerKm(120.0);
        rideRepository.save(rideCancelledByDriver);

        // Ride 11: CANCELLED_BY_PASSENGER (7 days ago) - passenger2 cancelled
        Ride rideCancelledByPassenger = new Ride();
        rideCancelledByPassenger.setDriver(driver);
        rideCancelledByPassenger.setPassengers(Collections.singleton(passenger2));
        rideCancelledByPassenger.setStatus(RideStatus.CANCELLED_BY_PASSENGER);
        rideCancelledByPassenger.setStartLocation(new Location("Zmaj Jovina, Novi Sad", 45.2551, 19.8450));
        rideCancelledByPassenger.setEndLocation(new Location("Sajmiste, Novi Sad", 45.2580, 19.8200));
        rideCancelledByPassenger.setStartTime(LocalDateTime.now().minusDays(7).minusHours(2));
        rideCancelledByPassenger.setEndTime(LocalDateTime.now().minusDays(7).minusHours(2).plusMinutes(3));
        rideCancelledByPassenger.setTotalCost(360.00);
        rideCancelledByPassenger.setEstimatedCost(360.00);
        rideCancelledByPassenger.setDistance(2.0);
        rideCancelledByPassenger.setRejectionReason("Changed plans");
        rideCancelledByPassenger.setRequestedVehicleType(VehicleType.STANDARD);
        rideCancelledByPassenger.setPanicPressed(false);
        rideCancelledByPassenger.setPaid(false);
        rideCancelledByPassenger.setPassengersExited(false);
        rideCancelledByPassenger.setDistanceTraveled(0.0);
        rideCancelledByPassenger.setPetTransport(false);
        rideCancelledByPassenger.setBabyTransport(false);
        rideCancelledByPassenger.setRateBaseFare(120.0);
        rideCancelledByPassenger.setRatePricePerKm(120.0);
        rideRepository.save(rideCancelledByPassenger);

        // Ride 12: FINISHED (10 days ago) - older ride for date range testing
        Ride rideOldFinished1 = new Ride();
        rideOldFinished1.setDriver(driver);
        rideOldFinished1.setPassengers(Collections.singleton(passenger3));
        rideOldFinished1.setStatus(RideStatus.FINISHED);
        rideOldFinished1.setStartLocation(new Location("Univerza, Novi Sad", 45.2461, 19.8519));
        rideOldFinished1.setEndLocation(new Location("Podbara, Novi Sad", 45.2620, 19.8420));
        rideOldFinished1.setStartTime(LocalDateTime.now().minusDays(10).minusHours(6));
        rideOldFinished1.setEndTime(LocalDateTime.now().minusDays(10).minusHours(5));
        rideOldFinished1.setTotalCost(180.00);
        rideOldFinished1.setEstimatedCost(180.00);
        rideOldFinished1.setDistance(0.5);
        rideOldFinished1.setRequestedVehicleType(VehicleType.STANDARD);
        rideOldFinished1.setPanicPressed(false);
        rideOldFinished1.setPaid(true);
        rideOldFinished1.setPassengersExited(true);
        rideOldFinished1.setDistanceTraveled(0.5);
        rideOldFinished1.setPetTransport(false);
        rideOldFinished1.setBabyTransport(false);
        rideOldFinished1.setRateBaseFare(120.0);
        rideOldFinished1.setRatePricePerKm(120.0);
        rideRepository.save(rideOldFinished1);

        // Ride 13: FINISHED (14 days ago) - for pagination & date filtering
        Ride rideOldFinished2 = new Ride();
        rideOldFinished2.setDriver(driver2);
        rideOldFinished2.setPassengers(Collections.singleton(passenger2));
        rideOldFinished2.setStatus(RideStatus.FINISHED);
        rideOldFinished2.setStartLocation(new Location("Kej, Novi Sad", 45.2555, 19.8620));
        rideOldFinished2.setEndLocation(new Location("Spens, Novi Sad", 45.2465, 19.8480));
        rideOldFinished2.setStartTime(LocalDateTime.now().minusDays(14).minusHours(3));
        rideOldFinished2.setEndTime(LocalDateTime.now().minusDays(14).minusHours(2));
        rideOldFinished2.setTotalCost(420.00);
        rideOldFinished2.setEstimatedCost(420.00);
        rideOldFinished2.setDistance(2.5);
        rideOldFinished2.setRequestedVehicleType(VehicleType.VAN);
        rideOldFinished2.setPanicPressed(false);
        rideOldFinished2.setPaid(true);
        rideOldFinished2.setPassengersExited(true);
        rideOldFinished2.setDistanceTraveled(2.5);
        rideOldFinished2.setPetTransport(true);
        rideOldFinished2.setBabyTransport(false);
        rideOldFinished2.setRateBaseFare(180.0);
        rideOldFinished2.setRatePricePerKm(120.0);
        rideRepository.save(rideOldFinished2);

        // Ride 14: CANCELLED (20 days ago) - old cancelled ride for date range edge case
        Ride rideOldCancelled = new Ride();
        rideOldCancelled.setDriver(driver);
        rideOldCancelled.setPassengers(Collections.singleton(passenger));
        rideOldCancelled.setStatus(RideStatus.CANCELLED);
        rideOldCancelled.setStartLocation(new Location("Novo Naselje, Novi Sad", 45.2350, 19.8100));
        rideOldCancelled.setEndLocation(new Location("Centar, Novi Sad", 45.2550, 19.8450));
        rideOldCancelled.setStartTime(LocalDateTime.now().minusDays(20).minusHours(4));
        rideOldCancelled.setEndTime(LocalDateTime.now().minusDays(20).minusHours(4).plusMinutes(2));
        rideOldCancelled.setTotalCost(720.00);
        rideOldCancelled.setEstimatedCost(720.00);
        rideOldCancelled.setDistance(5.0);
        rideOldCancelled.setRejectionReason("Schedule conflict");
        rideOldCancelled.setRequestedVehicleType(VehicleType.STANDARD);
        rideOldCancelled.setPanicPressed(false);
        rideOldCancelled.setPaid(false);
        rideOldCancelled.setPassengersExited(false);
        rideOldCancelled.setDistanceTraveled(0.0);
        rideOldCancelled.setPetTransport(false);
        rideOldCancelled.setBabyTransport(false);
        rideOldCancelled.setRateBaseFare(120.0);
        rideOldCancelled.setRatePricePerKm(120.0);
        rideRepository.save(rideOldCancelled);

        // ==========================================
        // 6. Create Reviews for finished rides
        // ==========================================
        
        // Review for rideFinished (passenger reviewing driver1)
        Review review1 = new Review();
        review1.setRide(rideFinished);
        review1.setPassenger(passenger);
        review1.setDriverRating(5);
        review1.setVehicleRating(4);
        review1.setComment("Great ride, very professional driver!");
        review1.setTimestamp(rideFinished.getEndTime().plusMinutes(5));
        reviewRepository.save(review1);

        // Review for rideFinished2 (passenger2 reviewing driver1)
        Review review2 = new Review();
        review2.setRide(rideFinished2);
        review2.setPassenger(passenger2);
        review2.setDriverRating(4);
        review2.setVehicleRating(4);
        review2.setComment("Good service, clean vehicle.");
        review2.setTimestamp(rideFinished2.getEndTime().plusMinutes(10));
        reviewRepository.save(review2);

        // Review for rideFinished3 (passenger reviewing driver2)
        Review review3 = new Review();
        review3.setRide(rideFinished3);
        review3.setPassenger(passenger);
        review3.setDriverRating(5);
        review3.setVehicleRating(5);
        review3.setComment("Excellent! Loved the spacious van.");
        review3.setTimestamp(rideFinished3.getEndTime().plusMinutes(15));
        reviewRepository.save(review3);

        // Review for rideMultiPassenger (passenger reviewing)
        Review review4 = new Review();
        review4.setRide(rideMultiPassenger);
        review4.setPassenger(passenger);
        review4.setDriverRating(4);
        review4.setVehicleRating(3);
        review4.setComment("Late night ride was okay.");
        review4.setTimestamp(rideMultiPassenger.getEndTime().plusMinutes(30));
        reviewRepository.save(review4);

        // Review for rideMultiPassenger (passenger2 reviewing)
        Review review5 = new Review();
        review5.setRide(rideMultiPassenger);
        review5.setPassenger(passenger2);
        review5.setDriverRating(5);
        review5.setVehicleRating(4);
        review5.setComment("Driver was friendly and got us home safely.");
        review5.setTimestamp(rideMultiPassenger.getEndTime().plusMinutes(35));
        reviewRepository.save(review5);

        // Review for ridePanicHistory (lower rating due to incident)
        Review review6 = new Review();
        review6.setRide(ridePanicHistory);
        review6.setPassenger(passenger3);
        review6.setDriverRating(2);
        review6.setVehicleRating(3);
        review6.setComment("Driver was driving too fast, had to use panic button.");
        review6.setTimestamp(ridePanicHistory.getEndTime().plusMinutes(20));
        reviewRepository.save(review6);

        // ==========================================
        // 7. Create Favorite Routes for passengers
        // ==========================================

        // Favorite route 1: passenger's daily commute
        FavoriteRoute favoriteRoute1 = new FavoriteRoute();
        favoriteRoute1.setUser(passenger);
        favoriteRoute1.setRouteName("Home to Work");
        favoriteRoute1.setStartLocation(new Location("Jevrejska 10, Novi Sad", 45.2550, 19.8430));
        favoriteRoute1.setEndLocation(new Location("Business Park, Novi Sad", 45.2600, 19.8200));
        favoriteRoute1.setStops(new ArrayList<>());
        favoriteRouteRepository.save(favoriteRoute1);

        // Favorite route 2: passenger's shopping route with stops
        FavoriteRoute favoriteRoute2 = new FavoriteRoute();
        favoriteRoute2.setUser(passenger);
        favoriteRoute2.setRouteName("Shopping Trip");
        favoriteRoute2.setStartLocation(new Location("Jevrejska 10, Novi Sad", 45.2550, 19.8430));
        favoriteRoute2.setEndLocation(new Location("Promenada Shopping Center, Novi Sad", 45.2420, 19.8520));
        favoriteRoute2.setStops(List.of(
                new Location("Big Fashion Mall, Novi Sad", 45.2480, 19.8380)
        ));
        favoriteRouteRepository.save(favoriteRoute2);

        // Favorite route 3: passenger2's route
        FavoriteRoute favoriteRoute3 = new FavoriteRoute();
        favoriteRoute3.setUser(passenger2);
        favoriteRoute3.setRouteName("Gym Route");
        favoriteRoute3.setStartLocation(new Location("Zmaj Jovina 5, Novi Sad", 45.2551, 19.8450));
        favoriteRoute3.setEndLocation(new Location("Spens, Novi Sad", 45.2465, 19.8480));
        favoriteRoute3.setStops(new ArrayList<>());
        favoriteRouteRepository.save(favoriteRoute3);

        // Favorite route 4: passenger3's airport route
        FavoriteRoute favoriteRoute4 = new FavoriteRoute();
        favoriteRoute4.setUser(passenger3);
        favoriteRoute4.setRouteName("Airport Transfer");
        favoriteRoute4.setStartLocation(new Location("Dunavska 20, Novi Sad", 45.2560, 19.8500));
        favoriteRoute4.setEndLocation(new Location("Aerodrom Nikola Tesla, Belgrade", 44.8184, 20.3091));
        favoriteRoute4.setStops(new ArrayList<>());
        favoriteRouteRepository.save(favoriteRoute4);

        // ==========================================
        // 8. Create Driver Activity Sessions
        // ==========================================

        // Driver 1: Currently active session (since 2h 23m ago - keeps under 5h limit)
        DriverActivitySession session1 = new DriverActivitySession();
        session1.setDriver(driver);
        session1.setStartTime(LocalDateTime.now().minusHours(2).minusMinutes(23));
        session1.setEndTime(null); // Still active
        driverActivitySessionRepository.save(session1);


        // ==========================================
        // 9. Create Panic records (historical)
        // ==========================================

        // Panic for ridePanicHistory
        Panic panic1 = new Panic();
        panic1.setUser(passenger3);
        panic1.setRide(ridePanicHistory);
        panic1.setReason("Passenger felt unsafe due to erratic driving");
        panic1.setTimestamp(ridePanicHistory.getStartTime().plusMinutes(15));
        panicRepository.save(panic1);

        // ==========================================
        // 10. Create Driver Change Request (sample)
        // ==========================================

        // Pending driver change request from driver2
        DriverChangeRequest changeRequest = new DriverChangeRequest();
        changeRequest.setRequestedDriverId(driver2.getId());
        changeRequest.setName("Michael");
        changeRequest.setSurname("Johnson");
        changeRequest.setEmail("driver2@example.com");
        changeRequest.setAddress("Nova Adresa 123, Novi Sad");
        changeRequest.setPhone("555-9999");
        changeRequest.setCreatedAt(LocalDateTime.now().minusDays(1));
        changeRequest.setStatus(DriverChangeStatus.PENDING);
        VehicleInformation vehicleInfo = new VehicleInformation();
        vehicleInfo.setModel("Mercedes Sprinter");
        vehicleInfo.setLicenseNumber("NEW-1234");
        vehicleInfo.setPassengerSeats(8);
        vehicleInfo.setBabyTransport(true);
        vehicleInfo.setPetTransport(true);
        vehicleInfo.setVehicleType(VehicleType.VAN);
        vehicleInfo.setDriverId(driver2.getId());
        changeRequest.setVehicle(vehicleInfo);
        driverChangeRequestRepository.save(changeRequest);

        System.out.println("Dummy data initialized.");
        System.out.println("===========================================");
        System.out.println("USERS:");
        System.out.println("  Driver 1: driver@example.com / password (isActive: true)");
        System.out.println("  Driver 2: driver2@example.com / password (isActive: false - offline)");
        System.out.println("  Passenger 1: passenger@example.com / password");
        System.out.println("  Passenger 2: passenger2@example.com / password");
        System.out.println("  Passenger 3: passenger3@example.com / password");
        System.out.println("  Admin: admin@example.com / password");
        System.out.println("===========================================");
        System.out.println("VEHICLES:");
        System.out.println("  Vehicle 1 (Driver 1): Toyota Camry - Status: BUSY (has active ride)");
        System.out.println("  Vehicle 2 (Driver 2): Mercedes Vito - Status: FREE");
        System.out.println("===========================================");
        System.out.println("RIDES:");
        System.out.println("  Active (IN_PROGRESS) Ride ID: " + rideActive.getId() + " - Start completed, heading to stop 1");
        System.out.println("  Scheduled Ride (Later Today) ID: " + ridePending.getId());
        System.out.println("  Scheduled Ride (Tomorrow) ID: " + rideScheduled.getId());
        System.out.println("  Finished Rides: " + rideFinished.getId() + ", " + rideFinished2.getId() + ", " + rideFinished3.getId() + ", " + rideMultiPassenger.getId() + ", " + ridePanicHistory.getId() + " (panic), " + rideOldFinished1.getId() + ", " + rideOldFinished2.getId());
        System.out.println("  Cancelled Ride ID: " + rideCancelled.getId() + ", " + rideOldCancelled.getId());
        System.out.println("  Cancelled By Driver Ride ID: " + rideCancelledByDriver.getId());
        System.out.println("  Cancelled By Passenger Ride ID: " + rideCancelledByPassenger.getId());
        System.out.println("===========================================");
        System.out.println("REVIEWS: " + reviewRepository.count() + " reviews created");
        System.out.println("FAVORITE ROUTES: " + favoriteRouteRepository.count() + " routes created");
        System.out.println("DRIVER ACTIVITY SESSIONS: " + driverActivitySessionRepository.count() + " sessions created");
        System.out.println("PANIC RECORDS: " + panicRepository.count() + " panic record(s) created");
        System.out.println("DRIVER CHANGE REQUESTS: " + driverChangeRequestRepository.count() + " request(s) created");
        System.out.println("===========================================");
    }
}
