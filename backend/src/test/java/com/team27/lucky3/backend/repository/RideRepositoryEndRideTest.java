package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.Location;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RideRepositoryEndRideTest {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    private User driver;
    private User passenger;

    @BeforeEach
    void setUp() {
        driver = new User();
        driver.setName("Driver");
        driver.setEmail("driver@example.com");
        driver.setRole(UserRole.DRIVER);
        driver = userRepository.save(driver);

        passenger = new User();
        passenger.setName("Passenger");
        passenger.setEmail("passenger@example.com");
        passenger.setRole(UserRole.PASSENGER);
        passenger = userRepository.save(passenger);
    }

    private Ride createRide(User d, RideStatus status, LocalDateTime start, LocalDateTime end) {
        Ride ride = new Ride();
        ride.setDriver(d);
        ride.setStatus(status);
        ride.setStartTime(start);
        ride.setEndTime(end);
        ride.setStartLocation(new Location("Start", 0.0, 0.0));
        ride.setEndLocation(new Location("End", 1.0, 1.0));
        ride.setPassengers(new java.util.HashSet<>(Set.of(passenger)));
        ride.setRequestedVehicleType(VehicleType.STANDARD);
        return rideRepository.save(ride);
    }

    @Test
    @DisplayName("findByDriverIdAndStatusInOrderByStartTimeAsc - returns correct rides ordered by startTime")
    void findNextRides_returnsOrderedRides() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.SCHEDULED, now.plusHours(2), null);
        createRide(driver, RideStatus.PENDING, now.plusHours(1), null);
        createRide(driver, RideStatus.FINISHED, now.minusHours(1), now);

        List<Ride> result = rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(
                driver.getId(), List.of(RideStatus.SCHEDULED, RideStatus.PENDING));

        assertEquals(2, result.size());
        assertEquals(RideStatus.PENDING, result.get(0).getStatus());
        assertEquals(RideStatus.SCHEDULED, result.get(1).getStatus());
    }

    @Test
    @DisplayName("findFinishedRidesByDriverSince - returns rides after threshold")
    void findFinishedRides_returnsRidesAfterThreshold() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusHours(5);

        createRide(driver, RideStatus.FINISHED, now.minusHours(2), now.minusHours(1));
        createRide(driver, RideStatus.FINISHED, now.minusHours(6), now.minusHours(5).minusMinutes(1));
        createRide(driver, RideStatus.IN_PROGRESS, now.minusHours(1), null);

        List<Ride> result = rideRepository.findFinishedRidesByDriverSince(driver.getId(), threshold);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getEndTime().isAfter(threshold));
    }

    @Test
    @DisplayName("existsByDriverIdAndStatusIn - returns true when matching ride exists")
    void existsByDriverIdAndStatusIn_returnsTrue() {
        createRide(driver, RideStatus.ACTIVE, LocalDateTime.now(), null);
        
        boolean exists = rideRepository.existsByDriverIdAndStatusIn(driver.getId(), List.of(RideStatus.ACTIVE));
        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByDriverIdAndStatusIn - returns false when no matching ride exists")
    void existsByDriverIdAndStatusIn_returnsFalse() {
        createRide(driver, RideStatus.FINISHED, LocalDateTime.now().minusHours(1), LocalDateTime.now());
        
        boolean exists = rideRepository.existsByDriverIdAndStatusIn(driver.getId(), List.of(RideStatus.ACTIVE));
        assertFalse(exists);
    }

    @Test
    @DisplayName("countCompletedRidesByDriverId - counts only FINISHED rides")
    void countCompletedRides_countsCorrectly() {
        createRide(driver, RideStatus.FINISHED, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
        createRide(driver, RideStatus.FINISHED, LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2));
        createRide(driver, RideStatus.CANCELLED, LocalDateTime.now().minusHours(1), LocalDateTime.now());
        
        Integer count = rideRepository.countCompletedRidesByDriverId(driver.getId());
        assertEquals(2, count);
    }

    @Test
    @DisplayName("sumTotalEarningsByDriverId - sums cost of only FINISHED rides")
    void sumTotalEarnings_sumsCorrectly() {
        Ride r1 = createRide(driver, RideStatus.FINISHED, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
        r1.setTotalCost(100.0);
        rideRepository.save(r1);
        
        Ride r2 = createRide(driver, RideStatus.FINISHED, LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2));
        r2.setTotalCost(150.0);
        rideRepository.save(r2);
        
        Ride r3 = createRide(driver, RideStatus.CANCELLED, LocalDateTime.now().minusHours(1), LocalDateTime.now());
        r3.setTotalCost(50.0);
        rideRepository.save(r3);
        
        Double sum = rideRepository.sumTotalEarningsByDriverId(driver.getId());
        assertEquals(250.0, sum);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Negative / Boundary Cases
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findByDriverIdAndStatusInOrderByStartTimeAsc - returns empty when no matching status")
    void findNextRides_noMatchingStatus_returnsEmpty() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.FINISHED, now.minusHours(1), now);
        createRide(driver, RideStatus.IN_PROGRESS, now, null);

        List<Ride> result = rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(
                driver.getId(), List.of(RideStatus.SCHEDULED, RideStatus.PENDING));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByDriverIdAndStatusInOrderByStartTimeAsc - returns empty for non-existent driver")
    void findNextRides_nonExistentDriver_returnsEmpty() {
        List<Ride> result = rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(
                99999L, List.of(RideStatus.SCHEDULED));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByDriverIdAndStatusInOrderByStartTimeAsc - does not return rides for a different driver")
    void findNextRides_differentDriverIsolation() {
        User driver2 = new User();
        driver2.setName("Driver2");
        driver2.setEmail("driver2@example.com");
        driver2.setRole(UserRole.DRIVER);
        driver2 = userRepository.save(driver2);

        createRide(driver2, RideStatus.SCHEDULED, LocalDateTime.now().plusHours(1), null);

        List<Ride> result = rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(
                driver.getId(), List.of(RideStatus.SCHEDULED, RideStatus.PENDING));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findFinishedRidesByDriverSince - excludes non-FINISHED rides")
    void findFinishedRides_excludesNonFinished() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.IN_PROGRESS, now.minusHours(1), null);
        createRide(driver, RideStatus.CANCELLED, now.minusHours(1), now);

        List<Ride> result = rideRepository.findFinishedRidesByDriverSince(driver.getId(), now.minusHours(5));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findFinishedRidesByDriverSince - returns empty for non-existent driver")
    void findFinishedRides_nonExistentDriver_returnsEmpty() {
        List<Ride> result = rideRepository.findFinishedRidesByDriverSince(99999L, LocalDateTime.now().minusHours(5));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("countCompletedRidesByDriverId - returns 0 when no FINISHED rides")
    void countCompletedRides_noFinished_returnsZero() {
        createRide(driver, RideStatus.IN_PROGRESS, LocalDateTime.now(), null);
        createRide(driver, RideStatus.CANCELLED, LocalDateTime.now().minusHours(1), LocalDateTime.now());

        Integer count = rideRepository.countCompletedRidesByDriverId(driver.getId());
        assertEquals(0, count);
    }

    @Test
    @DisplayName("countCompletedRidesByDriverId - returns 0 for non-existent driver")
    void countCompletedRides_nonExistentDriver_returnsZero() {
        Integer count = rideRepository.countCompletedRidesByDriverId(99999L);
        assertEquals(0, count);
    }

    @Test
    @DisplayName("sumTotalEarningsByDriverId - returns 0 when no FINISHED rides")
    void sumTotalEarnings_noFinished_returnsZero() {
        Ride r = createRide(driver, RideStatus.IN_PROGRESS, LocalDateTime.now(), null);
        r.setTotalCost(500.0);
        rideRepository.save(r);

        Double sum = rideRepository.sumTotalEarningsByDriverId(driver.getId());
        assertEquals(0.0, sum);
    }

    @Test
    @DisplayName("existsByDriverIdAndStatusIn - returns false for non-existent driver")
    void existsByDriverIdAndStatusIn_nonExistentDriver_returnsFalse() {
        boolean exists = rideRepository.existsByDriverIdAndStatusIn(99999L, List.of(RideStatus.ACTIVE));
        assertFalse(exists);
    }
}
