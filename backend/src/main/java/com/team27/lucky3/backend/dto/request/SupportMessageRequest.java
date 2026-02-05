package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending a support chat message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessageRequest {
    
    @NotBlank(message = "Message content is required")
    private String content;
}
