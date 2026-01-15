package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDriverChange {
    //@NotBlank(message = "Aproval decision is required") false po defaultu
    private boolean approve;
}
