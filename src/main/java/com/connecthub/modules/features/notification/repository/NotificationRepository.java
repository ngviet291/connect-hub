package com.connecthub.modules.features.notification.repository;

import com.connecthub.modules.features.notification.entity.Notification;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Long countUnreadByRecipientUsername(String username);


    Optional<Notification> findByIdAndRecipientUsername(UUID id, String recipientUsername);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.isRead = true
            WHERE n.recipient.username = :recipientUsername
    """)
    void markAsReadAllByIdAndRecipientUsername(String recipientUsername);


    @Query("""
            SELECT n
            FROM Notification n
            WHERE n.recipient.username = :username
            AND (:cursor IS NULL OR n.id < :cursor)
            ORDER BY n.id DESC
        """)
    List<Notification> findByRecipientUsername(String username, UUID cursor, Limit limit);
}
