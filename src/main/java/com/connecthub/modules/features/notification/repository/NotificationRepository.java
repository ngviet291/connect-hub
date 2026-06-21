package com.connecthub.modules.features.notification.repository;

import com.connecthub.modules.features.notification.entity.Notification;
import com.connecthub.modules.features.notification.enums.NotificationType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Long countUnreadByRecipientIdAndIsReadFalse(UUID recipientId);


    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.isRead = true
            WHERE n.recipient.id = :recipientId
    """)
    void markAsReadAllByIdAndRecipientId(UUID recipientId);


    @Query("""
            SELECT n
            FROM Notification n
            WHERE n.recipient.id = :recipientId
            AND (:type IS NULL OR n.type = :type)
            AND (:cursor IS NULL OR n.id < :cursor)
            ORDER BY n.id DESC
        """)
    List<Notification> findByRecipientIdAndType(UUID recipientId, UUID cursor, Limit limit, NotificationType type);
}
