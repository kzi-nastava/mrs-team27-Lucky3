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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RideRepository custom query methods.
 * Tests all methods except those directly inherited from JpaRepository.
 * Uses H2 in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RideRepositoryTest {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    private User driver;
    private User driver2;
    private User passenger;

    @BeforeAll
    void setUpUsers() {
        driver = new User();
        driver.setName("Driver");
        driver.setSurname("One");
        driver.setEmail("driver1@example.com");
        driver.setPassword("password");
        driver.setRole(UserRole.DRIVER);
        driver.setActive(true);
        driver.setEnabled(true);
        driver = userRepository.save(driver);

        driver2 = new User();
        driver2.setName("Driver");
        driver2.setSurname("Two");
        driver2.setEmail("driver2@example.com");
        driver2.setPassword("password");
        driver2.setRole(UserRole.DRIVER);
        driver2.setActive(true);
        driver2.setEnabled(true);
        driver2 = userRepository.save(driver2);

        passenger = new User();
        passenger.setName("Passenger");
        passenger.setSurname("One");
        passenger.setEmail("passenger1@example.com");
        passenger.setPassword("password");
        passenger.setRole(UserRole.PASSENGER);
        passenger.setEnabled(true);
        passenger = userRepository.save(passenger);
    }

    @AfterEach
    void cleanUpRides() {
        rideRepository.deleteAll();
    }

    // ─── Helper ────────────────────────────────────────────────────

    private Ride createRide(User rideDriver, RideStatus status, LocalDateTime startTime,
                            LocalDateTime endTime, Double totalCost) {
        Ride ride = new Ride();
        ride.setDriver(rideDriver);
        ride.setStatus(status);
        ride.setStartTime(startTime);
        ride.setEndTime(endTime);
        ride.setTotalCost(totalCost);
        ride.setEstimatedCost(100.0);
        ride.setDistance(10.0);
        ride.setRequestedVehicleType(VehicleType.STANDARD);
        ride.setStartLocation(new Location("Start Address", 45.2671, 19.8335));
        ride.setEndLocation(new Location("End Address", 45.2500, 19.8500));
        ride.setPassengers(Set.of(passenger));
        return rideRepository.save(ride);
    }

    // ═══════════════════════════════════════════════════════════════
    //  existsByDriverIdAndStatusIn
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("existsByDriverIdAndStatusIn - returns true when driver has ride with matching status")
    void existsByDriverIdAndStatusIn_matchingStatus_returnsTrue() {
        createRide(driver, RideStatus.IN_PROGRESS, LocalDateTime.now(), null, null);

        boolean result = rideRepository.existsByDriverIdAndStatusIn(
                driver.getId(), List.of(RideStatus.IN_PROGRESS, RideStatus.ACTIVE));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByDriverIdAndStatusIn - returns false when driver has no rides with matching status")
    void existsByDriverIdAndStatusIn_noMatchingStatus_returnsFalse() {
        createRide(driver, RideStatus.FINISHED, LocalDateTime.now().minusHours(1),
                LocalDateTime.now(), 200.0);

        boolean result = rideRepository.existsByDriverIdAndStatusIn(
                driver.getId(), List.of(RideStatus.IN_PROGRESS, RideStatus.ACTIVE));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByDriverIdAndStatusIn - returns false when driver has no rides at all")
    void existsByDriverIdAndStatusIn_noRides_returnsFalse() {
        boolean result = rideRepository.existsByDriverIdAndStatusIn(
                driver.getId(), List.of(RideStatus.IN_PROGRESS));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByDriverIdAndStatusIn - returns false for non-existent driver")
    void existsByDriverIdAndStatusIn_nonExistentDriver_returnsFalse() {
        boolean result = rideRepository.existsByDriverIdAndStatusIn(
                99999L, List.of(RideStatus.IN_PROGRESS));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByDriverIdAndStatusIn - checks only the specified driver, not others")
    void existsByDriverIdAndStatusIn_checksOnlySpecifiedDriver() {
        createRide(driver2, RideStatus.IN_PROGRESS, LocalDateTime.now(), null, null);

        boolean result = rideRepository.existsByDriverIdAndStatusIn(
                driver.getId(), List.of(RideStatus.IN_PROGRESS));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByDriverIdAndStatusIn - returns true when matching one of multiple statuses")
    void existsByDriverIdAndStatusIn_multipleStatuses_returnsTrue() {
        createRide(driver, RideStatus.ACCEPTED, LocalDateTime.now(), null, null);

        boolean result = rideRepository.existsByDriverIdAndStatusIn(
                driver.getId(), List.of(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS, RideStatus.ACTIVE));

        assertThat(result).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════
    //  findByDriverIdAndStatusInOrderByStartTimeAsc
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findByDriverIdAndStatusInOrderByStartTimeAsc - returns matching rides ordered by startTime")
    void findByDriverIdAndStatusInOrderByStartTimeAsc_returnsOrderedRides() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.SCHEDULED, now.plusHours(2), null, null);
        createRide(driver, RideStatus.PENDING, now.plusHours(1), null, null);
        createRide(driver, RideStatus.FINISHED, now.minusHours(1), now, 200.0);

        List<Ride> result = rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(
                driver.getId(), List.of(RideStatus.SCHEDULED, RideStatus.PENDING));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(RideStatus.PENDING);
        assertThat(result.get(1).getStatus()).isEqualTo(RideStatus.SCHEDULED);
        assertThat(result.get(0).getStartTime()).isBefore(result.get(1).getStartTime());
    }

    @Test
    @DisplayName("findByDriverIdAndStatusInOrderByStartTimeAsc - returns empty when no matching rides")
    void findByDriverIdAndStatusInOrderByStartTimeAsc_noMatch_returnsEmpty() {
        createRide(driver, RideStatus.FINISHED, LocalDateTime.now().minusHours(1),
                LocalDateTime.now(), 200.0);

        List<Ride> result = rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(
                driver.getId(), List.of(RideStatus.SCHEDULED, RideStatus.PENDING));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByDriverIdAndStatusInOrderByStartTimeAsc - returns empty for non-existent driver")
    void findByDriverIdAndStatusInOrderByStartTimeAsc_nonExistentDriver_returnsEmpty() {
        List<Ride> result = rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(
                99999L, List.of(RideStatus.SCHEDULED));

        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════
    //  findFinishedRidesByDriverSince
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findFinishedRidesByDriverSince - returns finished rides after the given time")
    void findFinishedRidesByDriverSince_returnsRidesAfterSince() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.FINISHED, now.minusHours(2), now.minusHours(1), 200.0);
        createRide(driver, RideStatus.FINISHED, now.minusHours(26), now.minusHours(25), 150.0);

        List<Ride> result = rideRepository.findFinishedRidesByDriverSince(driver.getId(), now.minusHours(24));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalCost()).isEqualTo(200.0);
    }

    @Test
    @DisplayName("findFinishedRidesByDriverSince - excludes non-FINISHED rides")
    void findFinishedRidesByDriverSince_excludesNonFinished() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.IN_PROGRESS, now.minusHours(1), null, null);
        createRide(driver, RideStatus.CANCELLED, now.minusHours(1), now, 0.0);

        List<Ride> result = rideRepository.findFinishedRidesByDriverSince(driver.getId(), now.minusHours(24));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findFinishedRidesByDriverSince - returns empty for non-existent driver")
    void findFinishedRidesByDriverSince_nonExistentDriver_returnsEmpty() {
        List<Ride> result = rideRepository.findFinishedRidesByDriverSince(99999L, LocalDateTime.now().minusHours(24));

        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════
    //  findDriversWithRidesInTimeRange
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findDriversWithRidesInTimeRange - returns drivers with overlapping rides")
    void findDriversWithRidesInTimeRange_overlapping_returnsDriverIds() {
        LocalDateTime now = LocalDateTime.now();
        Ride ride = createRide(driver, RideStatus.ACCEPTED, now, now.plusHours(1), null);

        List<Long> result = rideRepository.findDriversWithRidesInTimeRange(
                List.of(driver.getId()), now.plusMinutes(30), now.plusHours(2));

        assertThat(result).contains(driver.getId());
    }

    @Test
    @DisplayName("findDriversWithRidesInTimeRange - excludes FINISHED rides")
    void findDriversWithRidesInTimeRange_finishedRides_excludes() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.FINISHED, now, now.plusHours(1), 200.0);

        List<Long> result = rideRepository.findDriversWithRidesInTimeRange(
                List.of(driver.getId()), now.plusMinutes(30), now.plusHours(2));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findDriversWithRidesInTimeRange - excludes non-overlapping rides")
    void findDriversWithRidesInTimeRange_noOverlap_returnsEmpty() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.ACCEPTED, now, now.plusHours(1), null);

        List<Long> result = rideRepository.findDriversWithRidesInTimeRange(
                List.of(driver.getId()), now.plusHours(2), now.plusHours(3));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findDriversWithRidesInTimeRange - returns multiple busy drivers")
    void findDriversWithRidesInTimeRange_multipleBusyDrivers() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.ACCEPTED, now, now.plusHours(1), null);
        createRide(driver2, RideStatus.IN_PROGRESS, now, now.plusHours(1), null);

        List<Long> result = rideRepository.findDriversWithRidesInTimeRange(
                List.of(driver.getId(), driver2.getId()), now.plusMinutes(30), now.plusHours(2));

        assertThat(result).containsExactlyInAnyOrder(driver.getId(), driver2.getId());
    }

    // ═══════════════════════════════════════════════════════════════
    //  findAllInProgressRides
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findAllInProgressRides - returns rides with IN_PROGRESS and ACTIVE status that have a driver")
    void findAllInProgressRides_returnsInProgressAndActive() {
        createRide(driver, RideStatus.IN_PROGRESS, LocalDateTime.now(), null, null);
        createRide(driver2, RideStatus.ACTIVE, LocalDateTime.now(), null, null);
        createRide(driver, RideStatus.FINISHED, LocalDateTime.now().minusHours(1),
                LocalDateTime.now(), 200.0);

        List<Ride> result = rideRepository.findAllInProgressRides();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r ->
                r.getStatus() == RideStatus.IN_PROGRESS || r.getStatus() == RideStatus.ACTIVE);
    }

    @Test
    @DisplayName("findAllInProgressRides - returns empty when no in-progress rides")
    void findAllInProgressRides_noInProgress_returnsEmpty() {
        createRide(driver, RideStatus.FINISHED, LocalDateTime.now().minusHours(1),
                LocalDateTime.now(), 200.0);
        createRide(driver, RideStatus.PENDING, LocalDateTime.now(), null, null);

        List<Ride> result = rideRepository.findAllInProgressRides();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllInProgressRides - excludes rides without a driver")
    void findAllInProgressRides_noDriver_excluded() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setDriver(null);
        ride.setStartTime(LocalDateTime.now());
        ride.setStartLocation(new Location("Start", 45.0, 19.0));
        ride.setEndLocation(new Location("End", 45.1, 19.1));
        ride.setRequestedVehicleType(VehicleType.STANDARD);
        ride.setPassengers(Set.of(passenger));
        rideRepository.save(ride);

        List<Ride> result = rideRepository.findAllInProgressRides();

        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════
    //  countCompletedRidesByDriverId
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("countCompletedRidesByDriverId - counts only FINISHED rides for specified driver")
    void countCompletedRidesByDriverId_countsOnlyFinished() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.FINISHED, now.minusHours(3), now.minusHours(2), 200.0);
        createRide(driver, RideStatus.FINISHED, now.minusHours(5), now.minusHours(4), 150.0);
        createRide(driver, RideStatus.IN_PROGRESS, now, null, null);
        createRide(driver, RideStatus.CANCELLED, now.minusHours(1), now, 0.0);

        Integer count = rideRepository.countCompletedRidesByDriverId(driver.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countCompletedRidesByDriverId - returns 0 for driver with no finished rides")
    void countCompletedRidesByDriverId_noFinished_returnsZero() {
        createRide(driver, RideStatus.IN_PROGRESS, LocalDateTime.now(), null, null);

        Integer count = rideRepository.countCompletedRidesByDriverId(driver.getId());

        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("countCompletedRidesByDriverId - returns 0 for non-existent driver")
    void countCompletedRidesByDriverId_nonExistentDriver_returnsZero() {
        Integer count = rideRepository.countCompletedRidesByDriverId(99999L);

        assertThat(count).isEqualTo(0);
    }

    // ═══════════════════════════════════════════════════════════════
    //  sumTotalEarningsByDriverId
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("sumTotalEarningsByDriverId - sums totalCost of FINISHED rides")
    void sumTotalEarningsByDriverId_sumsFinished() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.FINISHED, now.minusHours(3), now.minusHours(2), 200.0);
        createRide(driver, RideStatus.FINISHED, now.minusHours(5), now.minusHours(4), 150.0);
        createRide(driver, RideStatus.CANCELLED, now.minusHours(1), now, 50.0);

        Double total = rideRepository.sumTotalEarningsByDriverId(driver.getId());

        assertThat(total).isEqualTo(350.0);
    }

    @Test
    @DisplayName("sumTotalEarningsByDriverId - returns 0 when no finished rides exist")
    void sumTotalEarningsByDriverId_noFinished_returnsZero() {
        createRide(driver, RideStatus.IN_PROGRESS, LocalDateTime.now(), null, null);

        Double total = rideRepository.sumTotalEarningsByDriverId(driver.getId());

        assertThat(total).isEqualTo(0.0);
    }

    // ═══════════════════════════════════════════════════════════════
    //  countActiveRides
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("countActiveRides - counts rides with active statuses")
    void countActiveRides_countsActiveStatuses() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.PENDING, now, null, null);
        createRide(driver, RideStatus.ACCEPTED, now, null, null);
        createRide(driver, RideStatus.IN_PROGRESS, now, null, null);
        createRide(driver, RideStatus.FINISHED, now.minusHours(1), now, 200.0);

        Integer count = rideRepository.countActiveRides();

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("countActiveRides - returns 0 when no active rides")
    void countActiveRides_noActive_returnsZero() {
        createRide(driver, RideStatus.FINISHED, LocalDateTime.now().minusHours(1),
                LocalDateTime.now(), 200.0);

        Integer count = rideRepository.countActiveRides();

        assertThat(count).isEqualTo(0);
    }

    // ═══════════════════════════════════════════════════════════════
    //  findAllActiveRidesForPassengerCount
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findAllActiveRidesForPassengerCount - returns IN_PROGRESS and ACTIVE rides")
    void findAllActiveRidesForPassengerCount_returnsCorrectStatuses() {
        createRide(driver, RideStatus.IN_PROGRESS, LocalDateTime.now(), null, null);
        createRide(driver2, RideStatus.ACTIVE, LocalDateTime.now(), null, null);
        createRide(driver, RideStatus.PENDING, LocalDateTime.now(), null, null);

        List<Ride> result = rideRepository.findAllActiveRidesForPassengerCount();

        assertThat(result).hasSize(2);
    }

    // ═══════════════════════════════════════════════════════════════
    //  findByScheduledTimeBetweenAndStatusIn
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findByScheduledTimeBetweenAndStatusIn - returns rides in time window with matching status")
    void findByScheduledTimeBetweenAndStatusIn_returnsMatchingRides() {
        LocalDateTime now = LocalDateTime.now();
        Ride scheduledRide = new Ride();
        scheduledRide.setDriver(driver);
        scheduledRide.setStatus(RideStatus.SCHEDULED);
        scheduledRide.setScheduledTime(now.plusMinutes(10));
        scheduledRide.setStartLocation(new Location("Start", 45.0, 19.0));
        scheduledRide.setEndLocation(new Location("End", 45.1, 19.1));
        scheduledRide.setRequestedVehicleType(VehicleType.STANDARD);
        scheduledRide.setPassengers(Set.of(passenger));
        rideRepository.save(scheduledRide);

        List<Ride> result = rideRepository.findByScheduledTimeBetweenAndStatusIn(
                now, now.plusMinutes(15), List.of(RideStatus.SCHEDULED));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findByScheduledTimeBetweenAndStatusIn - returns empty when no rides in window")
    void findByScheduledTimeBetweenAndStatusIn_noMatch_returnsEmpty() {
        LocalDateTime now = LocalDateTime.now();
        Ride scheduledRide = new Ride();
        scheduledRide.setDriver(driver);
        scheduledRide.setStatus(RideStatus.SCHEDULED);
        scheduledRide.setScheduledTime(now.plusHours(2));
        scheduledRide.setStartLocation(new Location("Start", 45.0, 19.0));
        scheduledRide.setEndLocation(new Location("End", 45.1, 19.1));
        scheduledRide.setRequestedVehicleType(VehicleType.STANDARD);
        scheduledRide.setPassengers(Set.of(passenger));
        rideRepository.save(scheduledRide);

        List<Ride> result = rideRepository.findByScheduledTimeBetweenAndStatusIn(
                now, now.plusMinutes(15), List.of(RideStatus.SCHEDULED));

        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════
    //  findByDriverIdAndStatusAndStartTimeAfterOrderByStartTimeAsc
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findByDriverIdAndStatusAndStartTimeAfterOrderByStartTimeAsc - returns future rides with status")
    void findByDriverIdAndStatusAndStartTimeAfter_returnsFutureRides() {
        LocalDateTime now = LocalDateTime.now();
        createRide(driver, RideStatus.SCHEDULED, now.plusHours(2), null, null);
        createRide(driver, RideStatus.SCHEDULED, now.plusHours(1), null, null);
        createRide(driver, RideStatus.FINISHED, now.minusHours(1), now, 200.0);

        List<Ride> result = rideRepository.findByDriverIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                driver.getId(), RideStatus.SCHEDULED, now);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStartTime()).isBefore(result.get(1).getStartTime());
    }

    @Test
    @DisplayName("findByDriverIdAndStatusAndStartTimeAfterOrderByStartTimeAsc - returns empty for past rides")
    void findByDriverIdAndStatusAndStartTimeAfter_pastRides_returnsEmpty() {
        createRide(driver, RideStatus.SCHEDULED, LocalDateTime.now().minusHours(1), null, null);

        List<Ride> result = rideRepository.findByDriverIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                driver.getId(), RideStatus.SCHEDULED, LocalDateTime.now());

        assertThat(result).isEmpty();
    }
}
