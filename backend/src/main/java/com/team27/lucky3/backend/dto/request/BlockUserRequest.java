package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Request DTO for blocking a user
public class BlockUserRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Block reason is required")
    @Size(max = 500, message = "Block reason cannot exceed 500 characters")
    private String reason;

    // Getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
