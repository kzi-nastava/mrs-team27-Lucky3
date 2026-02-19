package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.Message;
import com.team27.lucky3.backend.entity.SupportChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Find all messages for a support chat, ordered by timestamp.
     */
    List<Message> findBySupportChatOrderByTimestampAsc(SupportChat supportChat);

    /**
     * Find all messages for a support chat ID, ordered by timestamp.
     */
    List<Message> findBySupportChatIdOrderByTimestampAsc(Long supportChatId);
}
