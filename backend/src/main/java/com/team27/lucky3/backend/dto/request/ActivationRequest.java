package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivationRequest {
    @NotBlank(message = "Activation ID is required")
    private String activationId; // Token in main
}