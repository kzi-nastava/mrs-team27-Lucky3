package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.request.SupportMessageRequest;
import com.team27.lucky3.backend.dto.response.SupportChatListItemResponse;
import com.team27.lucky3.backend.dto.response.SupportChatResponse;
import com.team27.lucky3.backend.dto.response.SupportMessageResponse;
import com.team27.lucky3.backend.entity.Message;
import com.team27.lucky3.backend.entity.SupportChat;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.MessageRepository;
import com.team27.lucky3.backend.repository.SupportChatRepository;
import com.team27.lucky3.backend.service.NotificationService;
import com.team27.lucky3.backend.service.SupportChatService;
import com.team27.lucky3.backend.service.socket.SupportChatSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class SupportChatServiceImpl implements SupportChatService {

    private final SupportChatRepository supportChatRepository;
    private final MessageRepository messageRepository;
    private final SupportChatSocketService socketService;
    private final NotificationService notificationService;

    public SupportChatServiceImpl(
            SupportChatRepository supportChatRepository,
            MessageRepository messageRepository,
            @Lazy SupportChatSocketService socketService,
            @Lazy NotificationService notificationService) {
        this.supportChatRepository = supportChatRepository;
        this.messageRepository = messageRepository;
        this.socketService = socketService;
        this.notificationService = notificationService;
    }

    @Override
    public SupportChatResponse getOrCreateChatForUser(User user) {
        SupportChat chat = supportChatRepository.findByUserId(user.getId())
                .orElseGet(() -> createNewChatForUser(user));
        return mapToResponse(chat, true);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportChatResponse getChatById(Long chatId) {
        SupportChat chat = supportChatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Support chat not found with id: " + chatId));
        return mapToResponse(chat, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportChatListItemResponse> getAllChatsForAdmin() {
        return supportChatRepository.findAllWithMessagesOrderByLastMessageTimeDesc()
                .stream()
                .map(this::mapToListItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SupportMessageResponse sendUserMessage(User user, SupportMessageRequest request) {
        SupportChat chat = supportChatRepository.findByUserId(user.getId())
                .orElseGet(() -> createNewChatForUser(user));

        Message message = createMessage(user, chat, request.getContent(), false);
        chat.addMessage(message);
        chat.incrementUnreadCount();
        
        messageRepository.save(message);
        supportChatRepository.save(chat);

        SupportMessageResponse response = mapToMessageResponse(message);
        
        // Broadcast to admins about new message
        socketService.broadcastToAdmins(chat.getId(), response);
        // Also notify user's chat subscription
        socketService.broadcastToUserChat(chat.getId(), response);
        // Notify admins about updated chat list
        socketService.broadcastChatListUpdate(mapToListItemResponse(chat));

        // Send notification to all admins about the new support message
        notificationService.sendSupportMessageToAdmins(user, chat.getId(), request.getContent());

        log.info("User {} sent support message in chat {}", user.getEmail(), chat.getId());
        return response;
    }

    @Override
    public SupportMessageResponse sendAdminMessage(User admin, Long chatId, SupportMessageRequest request) {
        SupportChat chat = supportChatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Support chat not found with id: " + chatId));

        Message message = createMessage(admin, chat, request.getContent(), true);
        chat.addMessage(message);
        // Reset unread count when admin sends a message (implies they've read the chat)
        chat.resetUnreadCount();
        
        messageRepository.save(message);
        supportChatRepository.save(chat);

        SupportMessageResponse response = mapToMessageResponse(message);
        
        // Broadcast to the specific user's chat
        socketService.broadcastToUserChat(chat.getId(), response);
        // Also update admin views
        socketService.broadcastToAdmins(chat.getId(), response);
        // Notify about chat list update
        socketService.broadcastChatListUpdate(mapToListItemResponse(chat));
        // Notify the user via their personal topic (used by mobile AppNotificationManager)
        socketService.notifyUser(chat.getUser().getId(), response);

        log.info("Admin {} sent support message to chat {}", admin.getEmail(), chatId);
        return response;
    }

    @Override
    public void markChatAsRead(Long chatId) {
        SupportChat chat = supportChatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Support chat not found with id: " + chatId));
        
        chat.resetUnreadCount();
        supportChatRepository.save(chat);
        
        // Broadcast updated chat to admin list
        socketService.broadcastChatListUpdate(mapToListItemResponse(chat));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getChatMessages(Long chatId) {
        if (!supportChatRepository.existsById(chatId)) {
            throw new ResourceNotFoundException("Support chat not found with id: " + chatId);
        }
        
        return messageRepository.findBySupportChatIdOrderByTimestampAsc(chatId)
                .stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }

    // === Private Helper Methods ===

    private SupportChat createNewChatForUser(User user) {
        SupportChat chat = new SupportChat();
        chat.setUser(user);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setLastMessageTime(LocalDateTime.now());
        chat.setUnreadCount(0);
        return supportChatRepository.save(chat);
    }

    private Message createMessage(User sender, SupportChat chat, String content, boolean fromAdmin) {
        Message message = new Message();
        message.setSender(sender);
        message.setSupportChat(chat);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setFromAdmin(fromAdmin);
        message.setType("SUPPORT");
        return message;
    }

    private SupportChatResponse mapToResponse(SupportChat chat, boolean includeMessages) {
        User user = chat.getUser();
        
        SupportChatResponse.SupportChatResponseBuilder builder = SupportChatResponse.builder()
                .id(chat.getId())
                .userId(user.getId())
                .userName(user.getName() + " " + user.getSurname())
                .userEmail(user.getEmail())
                .userRole(user.getRole() == UserRole.ADMIN ? "ADMIN" : 
                          user.getRole() == UserRole.DRIVER ? "DRIVER" : "PASSENGER")
                .lastMessageTime(chat.getLastMessageTime())
                .unreadCount(chat.getUnreadCount())
                .createdAt(chat.getCreatedAt());

        // Get last message content
        List<Message> messages = chat.getMessages();
        if (messages != null && !messages.isEmpty()) {
            builder.lastMessage(messages.get(messages.size() - 1).getContent());
        }

        if (includeMessages) {
            List<SupportMessageResponse> messageResponses = messages.stream()
                    .map(this::mapToMessageResponse)
                    .collect(Collectors.toList());
            builder.messages(messageResponses);
        }

        return builder.build();
    }

    private SupportChatListItemResponse mapToListItemResponse(SupportChat chat) {
        User user = chat.getUser();
        String lastMessage = "";
        
        List<Message> messages = chat.getMessages();
        if (messages != null && !messages.isEmpty()) {
            lastMessage = messages.get(messages.size() - 1).getContent();
        }

        return SupportChatListItemResponse.builder()
                .id(chat.getId())
                .userId(user.getId())
                .userName(user.getName() + " " + user.getSurname())
                .userEmail(user.getEmail())
                .userRole(user.getRole() == UserRole.ADMIN ? "ADMIN" : 
                          user.getRole() == UserRole.DRIVER ? "DRIVER" : "PASSENGER")
                .lastMessage(lastMessage)
                .lastMessageTime(chat.getLastMessageTime())
                .unreadCount(chat.getUnreadCount())
                .build();
    }

    private SupportMessageResponse mapToMessageResponse(Message message) {
        User sender = message.getSender();
        return SupportMessageResponse.builder()
                .id(message.getId())
                .chatId(message.getSupportChat().getId())
                .senderId(sender.getId())
                .senderName(sender.getName() + " " + sender.getSurname())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .fromAdmin(message.isFromAdmin())
                .build();
    }
}
