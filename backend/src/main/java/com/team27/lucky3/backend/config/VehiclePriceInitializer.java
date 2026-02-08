package com.team27.lucky3.backend.config;

import com.team27.lucky3.backend.entity.VehiclePrice;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.repository.VehiclePriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VehiclePriceInitializer implements CommandLineRunner {

    private final VehiclePriceRepository vehiclePriceRepository;

    @Override
    public void run(String... args) {
        initPriceIfMissing(VehicleType.STANDARD, 120.0, 120.0);
        initPriceIfMissing(VehicleType.LUXURY, 360.0, 120.0);
        initPriceIfMissing(VehicleType.VAN, 180.0, 120.0);
    }

    private void initPriceIfMissing(VehicleType type, double baseFare, double pricePerKm) {
        if (vehiclePriceRepository.findByVehicleType(type).isEmpty()) {
            VehiclePrice price = new VehiclePrice();
            price.setVehicleType(type);
            price.setBaseFare(baseFare);
            price.setPricePerKm(pricePerKm);
            vehiclePriceRepository.save(price);
            log.info("Initialized default pricing for {}: baseFare={}, pricePerKm={}", type, baseFare, pricePerKm);
        }
    }
}
