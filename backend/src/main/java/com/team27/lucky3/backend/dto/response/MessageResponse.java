package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long senderId;
    private Long receiverId; // Null if it's general support chat
    private String message;
    private LocalDateTime timestamp;
    private String type; // "SUPPORT", "RIDE_CHAT"
}
