package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering/updating a user's FCM device token.
 * Sent by the mobile client after login or when the FCM token refreshes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequest {

    @NotBlank(message = "FCM token must not be blank")
    private String token;
}
