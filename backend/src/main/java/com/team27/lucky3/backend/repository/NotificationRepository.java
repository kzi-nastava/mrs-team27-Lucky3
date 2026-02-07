package com.team27.lucky3.backend.repository;

import com.team27.lucky3.backend.entity.Notification;
import com.team27.lucky3.backend.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Paginated history for a single user, newest first. */
    Page<Notification> findByRecipientIdOrderByTimestampDesc(Long recipientId, Pageable pageable);

    /** Unread count badge. */
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    /** Filtered by notification type for a specific user. */
    Page<Notification> findByRecipientIdAndTypeOrderByTimestampDesc(
            Long recipientId, NotificationType type, Pageable pageable);

    /** Bulk mark-all-read for a user. */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId AND n.isRead = false")
    int markAllReadForUser(@Param("userId") Long userId);
}
