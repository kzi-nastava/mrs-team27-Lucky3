package com.team27.lucky3.backend.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddRidePassengers {
    //linkovani korisnici za voznju preko emaila
    @NotEmpty
    @Valid
    private java.util.List<PassengerEmail> passengers;
}

