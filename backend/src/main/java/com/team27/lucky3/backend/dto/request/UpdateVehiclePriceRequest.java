package com.team27.lucky3.backend.dto.request;

import com.team27.lucky3.backend.entity.enums.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVehiclePriceRequest {
    @NotNull
    private VehicleType vehicleType;

    @DecimalMin("0.0")
    private Double pricePerKm;
}
