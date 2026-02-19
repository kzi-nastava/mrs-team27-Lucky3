package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideCancellationRequest {
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
