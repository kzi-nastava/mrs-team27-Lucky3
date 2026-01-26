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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        // 1b. Create Second Driver
        User driver2 = new User();
        driver2.setName("Mike");
        driver2.setSurname("Johnson");
        driver2.setEmail("driver2@example.com");
        driver2.setPassword(passwordEncoder.encode("password"));
        driver2.setRole(UserRole.DRIVER);
        driver2.setActive(true);
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

        // 4. Create Vehicle for Driver 1
        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setVehicleType(VehicleType.STANDARD);
        vehicle.setModel("Toyota Camry");
        vehicle.setLicensePlates("XYZ-1234");
        vehicle.setSeatCount(4);
        vehicle.setBabyTransport(true);
        vehicle.setPetTransport(false);
        // Vehicle location near Promenada (start of in-progress ride) but slightly ahead
        vehicle.setCurrentLocation(new Location("Near Univerzitetski Park, Novi Sad", 45.2450, 19.8515));
        vehicleRepository.save(vehicle);

        // 4b. Create Vehicle for Driver 2
        Vehicle vehicle2 = new Vehicle();
        vehicle2.setDriver(driver2);
        vehicle2.setVehicleType(VehicleType.VAN);
        vehicle2.setModel("Mercedes Vito");
        vehicle2.setLicensePlates("ABC-5678");
        vehicle2.setSeatCount(7);
        vehicle2.setBabyTransport(true);
        vehicle2.setPetTransport(true);
        vehicle2.setCurrentLocation(new Location("Spens, Novi Sad", 45.2465, 19.8480));
        vehicleRepository.save(vehicle2);

        // 5. Create Rides

        // Ride 1: PENDING (Scheduled later today) - Driver 1
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

        // Ride 2: IN_PROGRESS (Active now) - Driver 1
        // Start location completed (index -1), currently heading to first stop
        Ride rideActive = new Ride();
        rideActive.setDriver(driver);
        rideActive.setPassengers(Collections.singleton(passenger));
        rideActive.setStatus(RideStatus.IN_PROGRESS);
        rideActive.setStartLocation(new Location("Promenada Shopping Center, Novi Sad", 45.2420, 19.8520));
        rideActive.setEndLocation(new Location("Strand Beach, Novi Sad", 45.2429, 19.8424));
        rideActive.setStartTime(LocalDateTime.now().minusMinutes(10));
        rideActive.setEstimatedCost(320.00);
        rideActive.setDistance(2.1);
        rideActive.setTotalCost(320.00);
        rideActive.setPetTransport(false);
        rideActive.setBabyTransport(true);
        rideActive.setRequestedVehicleType(VehicleType.STANDARD);
        // Add multiple stops
        rideActive.setStops(List.of(
                new Location("Univerzitetski Park, Novi Sad", 45.2455, 19.8510),
                new Location("Dunavski Park, Novi Sad", 45.2560, 19.8520)
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
        rideFinished.setTotalCost(280.00);
        rideFinished.setDistance(1.5);
        rideFinished.setRequestedVehicleType(VehicleType.STANDARD);
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
        rideFinished2.setTotalCost(350.00);
        rideFinished2.setDistance(2.8);
        rideFinished2.setRequestedVehicleType(VehicleType.STANDARD);
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
        rideFinished3.setTotalCost(520.00);
        rideFinished3.setDistance(8.5);
        rideFinished3.setRequestedVehicleType(VehicleType.VAN);
        rideRepository.save(rideFinished3);

        // Ride 6: SCHEDULED (Tomorrow) - Driver 2 with passenger3
        Ride rideScheduled = new Ride();
        rideScheduled.setDriver(driver2);
        rideScheduled.setPassengers(Collections.singleton(passenger3));
        rideScheduled.setStatus(RideStatus.SCHEDULED);
        rideScheduled.setStartLocation(new Location("Aerodrom Nikola Tesla, Belgrade", 44.8184, 20.3091));
        rideScheduled.setEndLocation(new Location("Trg Republike, Novi Sad", 45.2551, 19.8450));
        rideScheduled.setScheduledTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0));
        rideScheduled.setEstimatedCost(2500.00);
        rideScheduled.setDistance(85.0);
        rideScheduled.setTotalCost(2500.00);
        rideScheduled.setPetTransport(false);
        rideScheduled.setBabyTransport(false);
        rideScheduled.setRequestedVehicleType(VehicleType.VAN);
        rideRepository.save(rideScheduled);

        // Ride 7: CANCELLED (Yesterday) - Driver 1 with passenger2
        Ride rideCancelled = new Ride();
        rideCancelled.setDriver(driver);
        rideCancelled.setPassengers(Collections.singleton(passenger2));
        rideCancelled.setStatus(RideStatus.CANCELLED);
        rideCancelled.setStartLocation(new Location("Liman 4, Novi Sad", 45.2350, 19.8250));
        rideCancelled.setEndLocation(new Location("Telep, Novi Sad", 45.2300, 19.8000));
        rideCancelled.setScheduledTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0));
        rideCancelled.setEstimatedCost(200.00);
        rideCancelled.setDistance(3.0);
        rideCancelled.setRejectionReason("Passenger cancelled");
        rideCancelled.setRequestedVehicleType(VehicleType.STANDARD);
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
        rideMultiPassenger.setTotalCost(180.00);
        rideMultiPassenger.setDistance(1.2);
        rideMultiPassenger.setRequestedVehicleType(VehicleType.STANDARD);
        rideRepository.save(rideMultiPassenger);

        System.out.println("Dummy data initialized.");
        System.out.println("===========================================");
        System.out.println("USERS:");
        System.out.println("  Driver 1: driver@example.com / password");
        System.out.println("  Driver 2: driver2@example.com / password");
        System.out.println("  Passenger 1: passenger@example.com / password");
        System.out.println("  Passenger 2: passenger2@example.com / password");
        System.out.println("  Passenger 3: passenger3@example.com / password");
        System.out.println("  Admin: admin@example.com / password");
        System.out.println("===========================================");
        System.out.println("RIDES:");
        System.out.println("  Active (IN_PROGRESS) Ride ID: " + rideActive.getId() + " - Start completed, heading to stop 1");
        System.out.println("  Pending Ride ID: " + ridePending.getId());
        System.out.println("  Scheduled Ride ID: " + rideScheduled.getId());
        System.out.println("  Finished Rides: " + rideFinished.getId() + ", " + rideFinished2.getId() + ", " + rideFinished3.getId() + ", " + rideMultiPassenger.getId());
        System.out.println("  Cancelled Ride ID: " + rideCancelled.getId());
        System.out.println("===========================================");
    }
}
