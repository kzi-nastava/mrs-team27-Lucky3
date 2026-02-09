package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a single support chat message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportMessageResponse {
    private Long id;
    private Long chatId;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
    private boolean fromAdmin;
}
