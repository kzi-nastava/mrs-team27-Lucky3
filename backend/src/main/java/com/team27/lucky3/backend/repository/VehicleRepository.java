package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByDriverId(Long driverId);

    // Fetch vehicles where the driver is marked as ACTIVE
    @Query("SELECT v FROM Vehicle v WHERE v.driver.isActive = true")
    List<Vehicle> findAllActiveVehicles();
}
