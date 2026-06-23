package com.connecthub.modules.features.chat.repository;

import com.connecthub.modules.features.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface MessageRepository extends JpaRepository<Message, UUID> {
    // DISTINCT ON là cú pháp PostgreSQL — lấy đúng 1 dòng mới nhất
    // (id DESC, vì id là UUIDv7 nên sort theo id = sort theo thời gian)
    // cho mỗi conversation_id, trong 1 lần quét duy nhất (không N+1).
    @Query(value = """
        SELECT DISTINCT ON (conversation_id) *
        FROM messages
        WHERE conversation_id IN :conversationIds
        ORDER BY conversation_id, id DESC
        """, nativeQuery = true)
    List<Message> findLastMessagesForConversations(@Param("conversationIds") List<UUID> conversationIds);

    Optional<Message> findById(UUID id);

    // unreadCount dùng chung cho PRIVATE và GROUP — chỉ so sánh id với
    // lastReadMessageId của member, KHÔNG join MessageReceipt. Đây là
    // truy vấn chạy thường xuyên nhất (mỗi lần load list conversations)
    // nên phải rẻ nhất có thể.
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.conversation.id = :conversationId
          AND m.sender.id <> :userId
          AND (:lastReadMessageId IS NULL OR m.id > :lastReadMessageId)
        """)
    long countUnreadSinceLastRead(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            @Param("lastReadMessageId") UUID lastReadMessageId
    );

    // GROUP: set cờ deliveredAt đúng 1 lần (lần delivered đầu tiên tới
    // BẤT KỲ ai). Điều kiện deliveredAt IS NULL đảm bảo các lần gọi sau
    // không ghi gì thêm — chi phí O(1) mỗi tin, không theo số thành viên.
    @Modifying
    @Query("UPDATE Message m SET m.deliveredAt = :now WHERE m.id = :messageId AND m.deliveredAt IS NULL")
    void markDeliveredFlag(@Param("messageId") UUID messageId, @Param("now") LocalDateTime now);

}
