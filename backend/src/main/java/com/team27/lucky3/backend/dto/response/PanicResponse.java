package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PanicResponse {
    private Long id;
    private UserResponse user;
    private RideResponse ride;
    private LocalDateTime time;
    private String reason;
}
