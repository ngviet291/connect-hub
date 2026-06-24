package com.connecthub.modules.features.chat.event;

import com.connecthub.common.websocket.event.DomainEvent;
import com.connecthub.modules.features.chat.dto.response.ConversationMemberResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateMemberRoleEvent implements DomainEvent {
    private UUID conversationId;
    private ConversationMemberResponse memberResponse;

}
