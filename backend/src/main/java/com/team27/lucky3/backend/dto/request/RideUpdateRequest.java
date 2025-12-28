package com.team27.lucky3.backend.dto.request;
 
import com.team27.lucky3.backend.dto.LocationDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideUpdateRequest {
    @NotBlank(message = "Status is required")
    private String status;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    private Boolean paid;

    private Boolean passengersExited;

    private LocationDto endLocation;
}