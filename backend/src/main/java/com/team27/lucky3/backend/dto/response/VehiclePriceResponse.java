package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.entity.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePriceResponse {
    private Long id;
    private VehicleType vehicleType;
    private Double baseFare;
    private Double pricePerKm;
}
