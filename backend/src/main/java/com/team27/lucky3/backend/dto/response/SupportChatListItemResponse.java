package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight response DTO for support chat list (without messages).
 * Used by admin to see all chats in the sidebar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportChatListItemResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userRole; // "PASSENGER" or "DRIVER"
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
}
