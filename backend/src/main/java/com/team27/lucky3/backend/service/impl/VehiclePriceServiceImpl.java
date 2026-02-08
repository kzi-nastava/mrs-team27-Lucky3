package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.entity.VehiclePrice;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.repository.VehiclePriceRepository;
import com.team27.lucky3.backend.service.VehiclePriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehiclePriceServiceImpl implements VehiclePriceService {

    private final VehiclePriceRepository vehiclePriceRepository;

    // Default fallback values (used only if DB has no entry)
    private static final double DEFAULT_BASE_FARE = 120.0;
    private static final double DEFAULT_PRICE_PER_KM = 120.0;

    @Override
    @Transactional(readOnly = true)
    public List<VehiclePrice> getAllPrices() {
        return vehiclePriceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public VehiclePrice getByVehicleType(VehicleType vehicleType) {
        return vehiclePriceRepository.findByVehicleType(vehicleType).orElse(null);
    }

    @Override
    @Transactional
    public VehiclePrice updatePrice(VehicleType vehicleType, Double baseFare, Double pricePerKm) {
        VehiclePrice price = vehiclePriceRepository.findByVehicleType(vehicleType)
                .orElseGet(() -> {
                    VehiclePrice newPrice = new VehiclePrice();
                    newPrice.setVehicleType(vehicleType);
                    return newPrice;
                });

        if (baseFare != null) {
            price.setBaseFare(baseFare);
        }
        if (pricePerKm != null) {
            price.setPricePerKm(pricePerKm);
        }

        return vehiclePriceRepository.save(price);
    }

    @Override
    @Transactional(readOnly = true)
    public double getBaseFare(VehicleType type) {
        if (type == null) return DEFAULT_BASE_FARE;
        return vehiclePriceRepository.findByVehicleType(type)
                .map(VehiclePrice::getBaseFare)
                .orElse(DEFAULT_BASE_FARE);
    }

    @Override
    @Transactional(readOnly = true)
    public double getPricePerKm(VehicleType type) {
        if (type == null) return DEFAULT_PRICE_PER_KM;
        return vehiclePriceRepository.findByVehicleType(type)
                .map(VehiclePrice::getPricePerKm)
                .orElse(DEFAULT_PRICE_PER_KM);
    }
}
