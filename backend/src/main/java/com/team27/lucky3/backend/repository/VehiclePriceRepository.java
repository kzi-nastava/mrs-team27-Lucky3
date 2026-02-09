package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.VehiclePrice;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehiclePriceRepository extends JpaRepository<VehiclePrice, Long> {
    Optional<VehiclePrice> findByVehicleType(VehicleType vehicleType);
}
