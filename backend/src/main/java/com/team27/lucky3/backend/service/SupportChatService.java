package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.SupportMessageRequest;
import com.team27.lucky3.backend.dto.response.SupportChatListItemResponse;
import com.team27.lucky3.backend.dto.response.SupportChatResponse;
import com.team27.lucky3.backend.dto.response.SupportMessageResponse;
import com.team27.lucky3.backend.entity.User;

import java.util.List;

/**
 * Service interface for support chat operations.
 */
public interface SupportChatService {

    /**
     * Get or create a support chat for the current user.
     * Each user has exactly one support chat.
     * @param user The user requesting their support chat
     * @return The user's support chat with all messages
     */
    SupportChatResponse getOrCreateChatForUser(User user);

    /**
     * Get a specific support chat by ID (admin only).
     * @param chatId The chat ID
     * @return The support chat with all messages
     */
    SupportChatResponse getChatById(Long chatId);

    /**
     * Get all support chats for admin view.
     * Returns chats ordered by last message time (newest first).
     * @return List of all support chats (without messages)
     */
    List<SupportChatListItemResponse> getAllChatsForAdmin();

    /**
     * Send a message from a user (driver/passenger) to their support chat.
     * @param user The user sending the message
     * @param request The message content
     * @return The created message
     */
    SupportMessageResponse sendUserMessage(User user, SupportMessageRequest request);

    /**
     * Send a message from an admin to a specific support chat.
     * @param admin The admin sending the message
     * @param chatId The target support chat ID
     * @param request The message content
     * @return The created message
     */
    SupportMessageResponse sendAdminMessage(User admin, Long chatId, SupportMessageRequest request);

    /**
     * Mark a chat as read by admin.
     * Resets the unread count to 0.
     * @param chatId The chat ID
     */
    void markChatAsRead(Long chatId);

    /**
     * Get messages for a specific chat.
     * @param chatId The chat ID
     * @return List of messages ordered by timestamp
     */
    List<SupportMessageResponse> getChatMessages(Long chatId);
}
