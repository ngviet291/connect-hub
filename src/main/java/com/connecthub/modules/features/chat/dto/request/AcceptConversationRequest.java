package com.connecthub.modules.features.chat.dto.request;

import com.connecthub.common.validation.anotation.RequiredUUID;
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
    @RequiredUUID
    private UUID conversationId;
    @RequiredUUID
    private UUID userAccept;
}
