package com.connecthub.modules.features.chat.dto.request;

import lombok.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class CreateGroupConversationRequest {
    private String name;
    private List<UUID> members;
}
