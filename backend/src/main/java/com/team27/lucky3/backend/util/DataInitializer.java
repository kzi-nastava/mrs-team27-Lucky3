package com.team27.lucky3.backend.util;

import com.team27.lucky3.backend.entity.*;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.repository.RideRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final RideRepository rideRepository;
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

        // 4. Create Vehicle for Driver
        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setVehicleType(VehicleType.STANDARD);
        vehicle.setModel("Toyota Camry");
        vehicle.setLicensePlates("XYZ-1234");
        vehicle.setSeatCount(4);
        vehicle.setBabyTransport(true);
        vehicle.setPetTransport(false);
        vehicle.setCurrentLocation(new Location("Zeleznicka Stanica, Novi Sad", 45.2650, 19.8290));
        vehicleRepository.save(vehicle);

        // 5. Create Rides

        // Ride 1: PENDING (Scheduled later today)
        Ride ridePending = new Ride();
        ridePending.setDriver(driver);
        ridePending.setPassengers(Collections.singleton(passenger));
        ridePending.setStatus(RideStatus.PENDING);
        ridePending.setStartLocation(new Location("Trg Slobode, Novi Sad", 45.2551, 19.8450));
        ridePending.setEndLocation(new Location("Petrovaradinska Tvrdjava, Novi Sad", 45.2530, 19.8610));
        ridePending.setScheduledTime(LocalDateTime.now().plusHours(2));
        ridePending.setEstimatedCost(450.00);
        ridePending.setDistance(3.2);
        ridePending.setTotalCost(450.00);
        ridePending.setPetTransport(false);
        ridePending.setBabyTransport(false);
        ridePending.setRequestedVehicleType(VehicleType.STANDARD);
        rideRepository.save(ridePending);

        // Ride 2: IN_PROGRESS (Active now) - This is likely the one users will test with /driver/ride/:id
        Ride rideActive = new Ride();
        rideActive.setDriver(driver);
        rideActive.setPassengers(Collections.singleton(passenger));
        rideActive.setStatus(RideStatus.IN_PROGRESS);
        rideActive.setStartLocation(new Location("Promenada Shopping Center, Novi Sad", 45.2420, 19.8520));
        rideActive.setEndLocation(new Location("Strand Beach, Novi Sad", 45.2429, 19.8424));
        rideActive.setStartTime(LocalDateTime.now().minusMinutes(10));
        rideActive.setEstimatedCost(320.00);
        rideActive.setDistance(2.1);
        rideActive.setTotalCost(320.00); // will be updated
        rideActive.setPetTransport(false);
        rideActive.setBabyTransport(true);
        rideActive.setRequestedVehicleType(VehicleType.STANDARD);
        // Important: Add stops if needed, or empty list
        rideActive.setStops(List.of(new Location("Univerzitetski Park, Novi Sad", 45.2455, 19.8510)));
        rideRepository.save(rideActive);

        // Ride 3: FINISHED
        Ride rideFinished = new Ride();
        rideFinished.setDriver(driver);
        rideFinished.setPassengers(Collections.singleton(passenger));
        rideFinished.setStatus(RideStatus.FINISHED);
        rideFinished.setStartLocation(new Location("Liman Park, Novi Sad", 45.248, 19.840));
        rideFinished.setEndLocation(new Location("Spens, Novi Sad", 45.2465, 19.8480));
        rideFinished.setStartTime(LocalDateTime.now().minusDays(1).minusHours(1));
        rideFinished.setEndTime(LocalDateTime.now().minusDays(1));
        rideFinished.setTotalCost(280.00);
        rideFinished.setDistance(1.5);
        rideRepository.save(rideFinished);

        System.out.println("Dummy data initialized. Driver: driver@example.com / password");
        System.out.println("Active Ride ID: " + rideActive.getId());
        System.out.println("Pending Ride ID: " + ridePending.getId());
    }
}
