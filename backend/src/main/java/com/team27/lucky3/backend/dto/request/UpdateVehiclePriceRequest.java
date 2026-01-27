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
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Price per kilometer is required")
    @DecimalMin(value = "0.0", message = "Price must be zero or positive")
    private Double pricePerKm;
}
