package com.connecthub.modules.features.chat.repository;

import com.connecthub.modules.features.chat.entity.MessageReceipt;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, UUID> {

    // MessageReceiptRepository
    @Query("""
            SELECT r.message.conversation.id, COUNT(r)
            FROM MessageReceipt r
            WHERE r.message.conversation.id IN :conversationIds
              AND r.user.id = :userId
              AND r.status <> :readStatus
            GROUP BY r.message.conversation.id
            """)
    List<Object[]> countUnreadGroupedByConversation(
            @Param("conversationIds") List<UUID> conversationIds,
            @Param("userId") UUID userId,
            @Param("readStatus") MessageStatus readStatus
    );

    // Chỉ dùng cho PRIVATE. Upsert: nếu chưa có receipt thì tạo mới
    // (status=DELIVERED); nếu đã có thì chỉ update khi đang KHÁC READ —
    // tránh 1 event DELIVERED muộn (race condition) hạ cấp ngược READ.
    @Modifying
    @Query(value = """
        INSERT INTO message_receipts (id, message_id, user_id, status, read_at, created_at, updated_at)
        VALUES (:id, :messageId, :userId, :status, :readAt, now(), now())
        ON CONFLICT (message_id, user_id)
        DO UPDATE SET status = EXCLUDED.status, read_at = EXCLUDED.read_at, updated_at = now()
        WHERE message_receipts.status <> 'READ'
        """, nativeQuery = true)
    void upsertStatus(
            @Param("id") UUID id,
            @Param("messageId") UUID messageId,
            @Param("userId") UUID userId,
            @Param("status") String status,
            @Param("readAt") LocalDateTime readAt
    );

    Optional<MessageReceipt> findByMessageIdAndUserId(UUID messageId, UUID userId);

    // Dùng khi hiển thị chi tiết 1 conversation PRIVATE (tick ✓✓ theo
    // từng tin) — không dùng cho tính unreadCount (đã chuyển qua
    // lastReadMessageId, xem MessageRepository).
    List<MessageReceipt> findByMessageIdIn(List<UUID> messageIds);
}
