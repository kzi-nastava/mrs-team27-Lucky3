package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.VehiclePrice;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VehiclePriceRepository custom query methods.
 * Tests all methods except those directly inherited from JpaRepository.
 * Uses H2 in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VehiclePriceRepositoryTest {

    @Autowired
    private VehiclePriceRepository vehiclePriceRepository;

    @AfterEach
    void cleanUp() {
        vehiclePriceRepository.deleteAll();
    }

    // ─── Helper ────────────────────────────────────────────────────
    private VehiclePrice createVehiclePrice(VehicleType type, Double baseFare, Double pricePerKm) {
        VehiclePrice price = new VehiclePrice();
        price.setVehicleType(type);
        price.setBaseFare(baseFare);
        price.setPricePerKm(pricePerKm);
        return vehiclePriceRepository.save(price);
    }

    // ═══════════════════════════════════════════════════════════════
    // findByVehicleType
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findByVehicleType - returns price for STANDARD vehicle type")
    void findByVehicleType_standardType_returnsPrice() {
        VehiclePrice standardPrice = createVehiclePrice(VehicleType.STANDARD, 100.0, 15.0);

        Optional<VehiclePrice> result = vehiclePriceRepository.findByVehicleType(VehicleType.STANDARD);

        assertTrue(result.isPresent());
        assertEquals(standardPrice.getId(), result.get().getId());
        assertEquals(VehicleType.STANDARD, result.get().getVehicleType());
        assertEquals(100.0, result.get().getBaseFare());
        assertEquals(15.0, result.get().getPricePerKm());
    }

    @Test
    @DisplayName("findByVehicleType - returns price for LUXURY vehicle type")
    void findByVehicleType_luxuryType_returnsPrice() {
        VehiclePrice luxuryPrice = createVehiclePrice(VehicleType.LUXURY, 200.0, 30.0);

        Optional<VehiclePrice> result = vehiclePriceRepository.findByVehicleType(VehicleType.LUXURY);

        assertTrue(result.isPresent());
        assertEquals(luxuryPrice.getId(), result.get().getId());
        assertEquals(VehicleType.LUXURY, result.get().getVehicleType());
        assertEquals(200.0, result.get().getBaseFare());
        assertEquals(30.0, result.get().getPricePerKm());
    }

    @Test
    @DisplayName("findByVehicleType - returns price for VAN vehicle type")
    void findByVehicleType_vanType_returnsPrice() {
        VehiclePrice vanPrice = createVehiclePrice(VehicleType.VAN, 150.0, 20.0);

        Optional<VehiclePrice> result = vehiclePriceRepository.findByVehicleType(VehicleType.VAN);

        assertTrue(result.isPresent());
        assertEquals(VehicleType.VAN, result.get().getVehicleType());
        assertEquals(150.0, result.get().getBaseFare());
        assertEquals(20.0, result.get().getPricePerKm());
    }

    @Test
    @DisplayName("findByVehicleType - returns empty when vehicle type not found")
    void findByVehicleType_notFound_returnsEmpty() {
        createVehiclePrice(VehicleType.STANDARD, 100.0, 15.0);

        Optional<VehiclePrice> result = vehiclePriceRepository.findByVehicleType(VehicleType.LUXURY);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByVehicleType - returns empty when no prices exist")
    void findByVehicleType_noPrices_returnsEmpty() {
        Optional<VehiclePrice> result = vehiclePriceRepository.findByVehicleType(VehicleType.STANDARD);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByVehicleType - returns correct price when multiple types exist")
    void findByVehicleType_multipleTypes_returnsCorrectPrice() {
        VehiclePrice standardPrice = createVehiclePrice(VehicleType.STANDARD, 100.0, 15.0);
        createVehiclePrice(VehicleType.LUXURY, 200.0, 30.0);
        createVehiclePrice(VehicleType.VAN, 150.0, 20.0);

        Optional<VehiclePrice> result = vehiclePriceRepository.findByVehicleType(VehicleType.STANDARD);

        assertTrue(result.isPresent());
        assertEquals(standardPrice.getId(), result.get().getId());
        assertEquals(VehicleType.STANDARD, result.get().getVehicleType());
    }

    @Test
    @DisplayName("findByVehicleType - handles null vehicle type gracefully")
    void findByVehicleType_nullType_returnsEmpty() {
        createVehiclePrice(VehicleType.STANDARD, 100.0, 15.0);

        Optional<VehiclePrice> result = vehiclePriceRepository.findByVehicleType(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByVehicleType - returns updated price after modification")
    void findByVehicleType_afterUpdate_returnsUpdatedPrice() {
        VehiclePrice standardPrice = createVehiclePrice(VehicleType.STANDARD, 100.0, 15.0);

        standardPrice.setBaseFare(120.0);
        standardPrice.setPricePerKm(18.0);
        vehiclePriceRepository.save(standardPrice);

        Optional<VehiclePrice> result = vehiclePriceRepository.findByVehicleType(VehicleType.STANDARD);

        assertTrue(result.isPresent());
        assertEquals(120.0, result.get().getBaseFare());
        assertEquals(18.0, result.get().getPricePerKm());
    }
}

