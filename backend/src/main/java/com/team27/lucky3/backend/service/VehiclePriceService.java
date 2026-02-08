package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.entity.VehiclePrice;
import com.team27.lucky3.backend.entity.enums.VehicleType;

import java.util.List;

public interface VehiclePriceService {
    List<VehiclePrice> getAllPrices();
    VehiclePrice getByVehicleType(VehicleType vehicleType);
    VehiclePrice updatePrice(VehicleType vehicleType, Double baseFare, Double pricePerKm);
    double getBaseFare(VehicleType type);
    double getPricePerKm(VehicleType type);
}
