package com.connecthub.modules.features.chat.service;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.chat.dto.request.SendMessageRequest;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.Message;
import com.connecthub.modules.features.chat.entity.MessageMedia;
import com.connecthub.modules.features.chat.exception.ReplyToMessageNotFoundException;
import com.connecthub.modules.features.chat.repository.MessageMediaRepository;
import com.connecthub.modules.features.chat.repository.MessageRepository;
import com.connecthub.modules.features.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageMediaRepository messageMediaRepository;

    @Transactional
    public Message saveMessage(Conversation conversation, User user, SendMessageRequest request) {
        Message message = Message.builder()
                .id(AppUtil.generateUUID())
                .conversation(conversation)
                .sender(user)
                .content(request.getContent())
                .build();

        if (request.getReplyToMessageId() != null) {
            Message replyToMessage = messageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new ReplyToMessageNotFoundException(request.getReplyToMessageId().toString()));
            message.setReplyTo(replyToMessage);
        }

        boolean hasMedia = request.getMedia() != null && !request.getMedia().isEmpty();
        messageRepository.save(message);

        if (hasMedia) {
            List<MessageMedia> mediaList = request.getMedia().stream()
                    .map(mediaRequest -> MessageMedia.builder()
                            .id(AppUtil.generateUUID())
                            .message(message)
                            .url(mediaRequest.getUrl())
                            .type(mediaRequest.getType())
                            .build())
                    .toList();
            messageMediaRepository.saveAll(mediaList);
        }

        // Không tạo MessageReceipt ở đây. SENT được suy ra từ việc Message
        // tồn tại; DELIVERED ghi đúng lúc push WS thành công (xem
        // DeliveryTrackingService.markDelivered), không phải lúc gửi.
        return message;
    }



}
