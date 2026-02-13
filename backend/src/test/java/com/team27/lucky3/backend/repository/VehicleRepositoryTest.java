package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.Vehicle;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.VehicleStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.entity.enums.UserRole;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VehicleRepository custom query methods.
 * Tests all methods except those directly inherited from JpaRepository.
 * Uses H2 in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    private User activeDriver;
    private User inactiveDriver;
    private User anotherActiveDriver;

    @BeforeAll
    void setUpUsers() {
        activeDriver = new User();
        activeDriver.setName("Active");
        activeDriver.setSurname("Driver");
        activeDriver.setEmail("active.driver@example.com");
        activeDriver.setPassword("password");
        activeDriver.setRole(UserRole.DRIVER);
        activeDriver.setActive(true);
        activeDriver.setEnabled(true);
        activeDriver = userRepository.save(activeDriver);

        inactiveDriver = new User();
        inactiveDriver.setName("Inactive");
        inactiveDriver.setSurname("Driver");
        inactiveDriver.setEmail("inactive.driver@example.com");
        inactiveDriver.setPassword("password");
        inactiveDriver.setRole(UserRole.DRIVER);
        inactiveDriver.setActive(false);
        inactiveDriver.setEnabled(true);
        inactiveDriver = userRepository.save(inactiveDriver);

        anotherActiveDriver = new User();
        anotherActiveDriver.setName("Another");
        anotherActiveDriver.setSurname("Active");
        anotherActiveDriver.setEmail("another.active@example.com");
        anotherActiveDriver.setPassword("password");
        anotherActiveDriver.setRole(UserRole.DRIVER);
        anotherActiveDriver.setActive(true);
        anotherActiveDriver.setEnabled(true);
        anotherActiveDriver = userRepository.save(anotherActiveDriver);
    }

    @AfterEach
    void cleanUpVehicles() {
        vehicleRepository.deleteAll();
    }

    // ─── Helper ────────────────────────────────────────────────────
    private Vehicle createVehicle(User driver, VehicleStatus status, VehicleType type,
                                  String licensePlate, String model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setStatus(status);
        vehicle.setVehicleType(type);
        vehicle.setLicensePlates(licensePlate);
        vehicle.setModel(model);
        vehicle.setSeatCount(4);
        vehicle.setBabyTransport(false);
        vehicle.setPetTransport(false);
        return vehicleRepository.save(vehicle);
    }

    // ═══════════════════════════════════════════════════════════════
    // findByDriverId
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findByDriverId - returns vehicle when driver has one")
    void findByDriverId_driverHasVehicle_returnsVehicle() {
        Vehicle vehicle = createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-123-AB", "Toyota Camry");

        Optional<Vehicle> result = vehicleRepository.findByDriverId(activeDriver.getId());

        assertTrue(result.isPresent());
        assertEquals(vehicle.getId(), result.get().getId());
        assertEquals("BG-123-AB", result.get().getLicensePlates());
        assertEquals(activeDriver.getId(), result.get().getDriver().getId());
    }

    @Test
    @DisplayName("findByDriverId - returns empty when driver has no vehicle")
    void findByDriverId_noVehicle_returnsEmpty() {
        Optional<Vehicle> result = vehicleRepository.findByDriverId(activeDriver.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByDriverId - returns empty for non-existent driver")
    void findByDriverId_nonExistentDriver_returnsEmpty() {
        Optional<Vehicle> result = vehicleRepository.findByDriverId(99999L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByDriverId - returns correct vehicle when multiple drivers have vehicles")
    void findByDriverId_multipleDrivers_returnsCorrectVehicle() {
        Vehicle vehicle1 = createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Toyota Camry");
        Vehicle vehicle2 = createVehicle(anotherActiveDriver, VehicleStatus.FREE,
                VehicleType.LUXURY, "BG-222-BB", "Mercedes S-Class");

        Optional<Vehicle> result = vehicleRepository.findByDriverId(activeDriver.getId());

        assertTrue(result.isPresent());
        assertEquals(vehicle1.getId(), result.get().getId());
        assertEquals("BG-111-AA", result.get().getLicensePlates());
        assertNotEquals(vehicle2.getId(), result.get().getId());
    }

    @Test
    @DisplayName("findByDriverId - returns vehicle for inactive driver")
    void findByDriverId_inactiveDriver_returnsVehicle() {
        Vehicle vehicle = createVehicle(inactiveDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-333-CC", "Honda Civic");

        Optional<Vehicle> result = vehicleRepository.findByDriverId(inactiveDriver.getId());

        assertTrue(result.isPresent());
        assertEquals(vehicle.getId(), result.get().getId());
    }

    @Test
    @DisplayName("findByDriverId - handles null driver ID gracefully")
    void findByDriverId_nullDriverId_returnsEmpty() {
        Optional<Vehicle> result = vehicleRepository.findByDriverId(null);

        assertTrue(result.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════
    // findByStatus
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findByStatus - returns vehicles with matching status")
    void findByStatus_matchingStatus_returnsVehicles() {
        createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Toyota Camry");
        createVehicle(anotherActiveDriver, VehicleStatus.FREE,
                VehicleType.LUXURY, "BG-222-BB", "Mercedes S-Class");
        createVehicle(inactiveDriver, VehicleStatus.BUSY,
                VehicleType.STANDARD, "BG-333-CC", "Honda Civic");

        List<Vehicle> result = vehicleRepository.findByStatus(VehicleStatus.FREE);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(v -> v.getStatus() == VehicleStatus.FREE));
    }

    @Test
    @DisplayName("findByStatus - returns empty list when no vehicles match status")
    void findByStatus_noMatch_returnsEmpty() {
        createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Toyota Camry");

        List<Vehicle> result = vehicleRepository.findByStatus(VehicleStatus.BUSY);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByStatus - returns empty list when no vehicles exist")
    void findByStatus_noVehicles_returnsEmpty() {
        List<Vehicle> result = vehicleRepository.findByStatus(VehicleStatus.FREE);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByStatus - returns all vehicles with BUSY status")
    void findByStatus_unFREEStatus_returnsUnFREEVehicles() {
        createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Toyota Camry");
        createVehicle(inactiveDriver, VehicleStatus.BUSY,
                VehicleType.STANDARD, "BG-222-BB", "Honda Civic");
        createVehicle(anotherActiveDriver, VehicleStatus.BUSY,
                VehicleType.LUXURY, "BG-333-CC", "BMW 5 Series");

        List<Vehicle> result = vehicleRepository.findByStatus(VehicleStatus.BUSY);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(v -> v.getStatus() == VehicleStatus.BUSY));
    }

    @Test
    @DisplayName("findByStatus - includes vehicles from both active and inactive drivers")
    void findByStatus_mixedDriverActivity_returnsAllMatching() {
        createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Toyota Camry");
        createVehicle(inactiveDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-222-BB", "Honda Civic");

        List<Vehicle> result = vehicleRepository.findByStatus(VehicleStatus.FREE);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findByStatus - handles null status gracefully")
    void findByStatus_nullStatus_returnsEmpty() {
        createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Toyota Camry");

        List<Vehicle> result = vehicleRepository.findByStatus(null);

        assertTrue(result.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════
    // findAllActiveVehicles
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findAllActiveVehicles - returns only vehicles with active drivers")
    void findAllActiveVehicles_activeDrivers_returnsVehicles() {
        createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Toyota Camry");
        createVehicle(anotherActiveDriver, VehicleStatus.BUSY,
                VehicleType.LUXURY, "BG-222-BB", "Mercedes S-Class");
        createVehicle(inactiveDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-333-CC", "Honda Civic");

        List<Vehicle> result = vehicleRepository.findAllActiveVehicles();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(v -> v.getDriver().isActive()));
        assertTrue(result.stream().noneMatch(v -> v.getDriver().getId().equals(inactiveDriver.getId())));
    }

    @Test
    @DisplayName("findAllActiveVehicles - excludes vehicles with inactive drivers")
    void findAllActiveVehicles_inactiveDrivers_excludes() {
        createVehicle(inactiveDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Honda Civic");

        List<Vehicle> result = vehicleRepository.findAllActiveVehicles();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findAllActiveVehicles - returns empty when no vehicles exist")
    void findAllActiveVehicles_noVehicles_returnsEmpty() {
        List<Vehicle> result = vehicleRepository.findAllActiveVehicles();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findAllActiveVehicles - includes vehicles regardless of vehicle status")
    void findAllActiveVehicles_differentVehicleStatuses_includesAll() {
        createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Toyota Camry");
        createVehicle(anotherActiveDriver, VehicleStatus.BUSY,
                VehicleType.LUXURY, "BG-222-BB", "Mercedes S-Class");

        List<Vehicle> result = vehicleRepository.findAllActiveVehicles();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(v -> v.getStatus() == VehicleStatus.FREE));
        assertTrue(result.stream().anyMatch(v -> v.getStatus() == VehicleStatus.BUSY));
    }

    @Test
    @DisplayName("findAllActiveVehicles - returns only active when mix of active and inactive exist")
    void findAllActiveVehicles_mixedDrivers_returnsOnlyActive() {
        Vehicle activeVehicle1 = createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Toyota Camry");
        Vehicle activeVehicle2 = createVehicle(anotherActiveDriver, VehicleStatus.FREE,
                VehicleType.LUXURY, "BG-222-BB", "Mercedes S-Class");
        createVehicle(inactiveDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-333-CC", "Honda Civic");

        List<Vehicle> result = vehicleRepository.findAllActiveVehicles();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(v -> v.getId().equals(activeVehicle1.getId())));
        assertTrue(result.stream().anyMatch(v -> v.getId().equals(activeVehicle2.getId())));
    }

    @Test
    @DisplayName("findAllActiveVehicles - verifies driver active flag is checked correctly")
    void findAllActiveVehicles_driverActiveFlag_checksCorrectly() {
        // Create vehicle for active driver
        createVehicle(activeDriver, VehicleStatus.FREE,
                VehicleType.STANDARD, "BG-111-AA", "Toyota Camry");

        List<Vehicle> resultBefore = vehicleRepository.findAllActiveVehicles();
        assertEquals(1, resultBefore.size());

        // Deactivate the driver
        activeDriver.setActive(false);
        userRepository.save(activeDriver);

        List<Vehicle> resultAfter = vehicleRepository.findAllActiveVehicles();
        assertTrue(resultAfter.isEmpty());

        // Activate again
        activeDriver.setActive(true);
        userRepository.save(activeDriver);
    }
}
