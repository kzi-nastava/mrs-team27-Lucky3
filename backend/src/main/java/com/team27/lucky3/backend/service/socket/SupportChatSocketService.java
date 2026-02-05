package com.team27.lucky3.backend.service.socket;

import com.team27.lucky3.backend.dto.response.SupportChatListItemResponse;
import com.team27.lucky3.backend.dto.response.SupportMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for broadcasting support chat updates via WebSocket.
 * 
 * Topics:
 * - /topic/support/chat/{chatId} - Messages for a specific chat (user subscribes to their chat)
 * - /topic/support/admin/messages - New messages notification for admins
 * - /topic/support/admin/chats - Chat list updates for admins
 */
@Service
@Slf4j
public class SupportChatSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public SupportChatSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast a new message to a specific chat's subscribers.
     * Users subscribe to their own chat to receive real-time messages.
     * 
     * @param chatId The support chat ID
     * @param message The message to broadcast
     */
    public void broadcastToUserChat(Long chatId, SupportMessageResponse message) {
        String destination = "/topic/support/chat/" + chatId;
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Broadcasted message to {}", destination);
    }

    /**
     * Broadcast a new message notification to all admins.
     * Admins receive this to update their view when new messages arrive.
     * 
     * @param chatId The chat that received the message
     * @param message The new message
     */
    public void broadcastToAdmins(Long chatId, SupportMessageResponse message) {
        String destination = "/topic/support/admin/messages";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Broadcasted admin notification for chat {}", chatId);
    }

    /**
     * Broadcast chat list update to admins.
     * Called when a new message is sent to update unread counts and order.
     * 
     * @param chatUpdate The updated chat summary
     */
    public void broadcastChatListUpdate(SupportChatListItemResponse chatUpdate) {
        String destination = "/topic/support/admin/chats";
        messagingTemplate.convertAndSend(destination, chatUpdate);
        log.debug("Broadcasted chat list update for chat {}", chatUpdate.getId());
    }

    /**
     * Notify a specific user that they have a new support message.
     * Can be used for push notifications or badge updates.
     * 
     * @param userId The user to notify
     * @param message The new message
     */
    public void notifyUser(Long userId, SupportMessageResponse message) {
        String destination = "/topic/support/user/" + userId + "/notification";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Sent notification to user {}", userId);
    }
}
