package org.example.backend.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateVehicleDTO{
    @NotBlank
    private String model;

    @NotBlank
    private String type; // SEDAN, HATCHBACK, SUV, VAN...

    @NotBlank
    private String licensePlateNumber;

    @NotNull
    @Min(1)
    private Integer seatsCount;

    @NotNull
    private Boolean babyTransportEnabled;

    @NotNull
    private Boolean petTransportEnabled;
}
