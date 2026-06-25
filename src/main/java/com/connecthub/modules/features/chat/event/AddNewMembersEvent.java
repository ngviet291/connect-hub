package com.connecthub.modules.features.chat.event;

import com.connecthub.common.websocket.event.DomainEvent;
import com.connecthub.modules.features.chat.dto.response.ConversationMemberResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AddNewMembersEvent implements DomainEvent {
    private UUID conversationId;
    private List<ConversationMemberResponse> newMembers;
}
