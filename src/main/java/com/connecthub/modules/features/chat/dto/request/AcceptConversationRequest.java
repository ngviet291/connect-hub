package com.connecthub.modules.features.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AcceptConversationRequest {
    private UUID conversationId;
    private UUID userAccept;
}
