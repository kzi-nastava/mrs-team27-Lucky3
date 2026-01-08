package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    @NotBlank(message = "Message content is required")
    private String message;

    @NotNull(message = "Type is required")
    private String type;
}
