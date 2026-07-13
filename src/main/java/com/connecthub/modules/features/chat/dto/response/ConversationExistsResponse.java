package com.connecthub.modules.features.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationExistsResponse {
    private UUID conversationId; // null nếu chưa từng nhắn tin
}