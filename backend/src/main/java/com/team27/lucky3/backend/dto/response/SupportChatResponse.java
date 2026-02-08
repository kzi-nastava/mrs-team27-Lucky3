package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for a support chat (used by admin to see chat list).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportChatResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userRole; // "PASSENGER" or "DRIVER"
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
    private LocalDateTime createdAt;
    private List<SupportMessageResponse> messages;
}
