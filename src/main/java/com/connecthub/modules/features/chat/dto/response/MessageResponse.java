package com.connecthub.modules.features.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MessageResponse {
    private String messageId;
    private String conversationId;
    private String senderId;
    private String recipientIs;

}
