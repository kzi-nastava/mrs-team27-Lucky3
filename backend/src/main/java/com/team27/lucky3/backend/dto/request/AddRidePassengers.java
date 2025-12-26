package com.team27.lucky3.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddRidePassengers {
    @NotEmpty
    @Valid
    private List<PassengerEmail> passengers;
}

