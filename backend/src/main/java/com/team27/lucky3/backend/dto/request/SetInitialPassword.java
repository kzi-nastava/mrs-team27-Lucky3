package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetInitialPassword {
    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    private String confirmPassword;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}

