package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InconsistencyRequest {
    @NotBlank(message = "Remark is required")
    @Size(max = 500, message = "Remark cannot exceed 500 characters")
    private String remark;
}
