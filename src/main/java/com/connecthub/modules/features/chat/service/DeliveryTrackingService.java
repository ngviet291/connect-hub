package com.connecthub.modules.features.chat.service;

import com.connecthub.modules.features.chat.entity.Message;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import com.connecthub.modules.features.chat.repository.MessageReceiptRepository;
import com.connecthub.modules.features.chat.repository.MessageRepository;
import com.connecthub.common.util.AppUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryTrackingService {

    private final MessageReceiptRepository messageReceiptRepository;
    private final MessageRepository messageRepository;

    /**
     * Gọi đúng lúc WS đã thực sự gửi tới recipient (trong
     * MessageNotificationHandler.handle, ngay sau convertAndSend/...ToUser).
     * PRIVATE: ghi receipt riêng cho recipient đó (rẻ, chỉ 1 row, upsert).
     * GROUP: chỉ set cờ deliveredAt chung trên Message (O(1) mỗi tin,
     * không tăng theo số thành viên) — KHÔNG ghi receipt riêng từng người.
     */
    @Transactional
    public void markDelivered(UUID messageId, ConversationType conversationType, UUID recipientId) {
        if (conversationType == ConversationType.PRIVATE) {
            messageReceiptRepository.upsertStatus(
                    AppUtil.generateUUID(),
                    messageId,
                    recipientId,
                    MessageStatus.DELIVERED.name(),
                    null
            );
        } else {
            messageRepository.markDeliveredFlag(messageId, LocalDateTime.now());
        }
    }
}