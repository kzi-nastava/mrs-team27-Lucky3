package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.SupportChat;
import com.team27.lucky3.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportChatRepository extends JpaRepository<SupportChat, Long> {
    
    /**
     * Find support chat by user.
     */
    Optional<SupportChat> findByUser(User user);

    /**
     * Find support chat by user ID.
     */
    Optional<SupportChat> findByUserId(Long userId);

    /**
     * Check if a support chat exists for a user.
     */
    boolean existsByUserId(Long userId);

    /**
     * Find all support chats ordered by last message time (newest first).
     * Used by admin to see all chats.
     */
    List<SupportChat> findAllByOrderByLastMessageTimeDesc();

    /**
     * Find all support chats that have messages, ordered by last message time.
     */
    @Query("SELECT sc FROM SupportChat sc WHERE sc.lastMessageTime IS NOT NULL ORDER BY sc.lastMessageTime DESC")
    List<SupportChat> findAllWithMessagesOrderByLastMessageTimeDesc();
}
