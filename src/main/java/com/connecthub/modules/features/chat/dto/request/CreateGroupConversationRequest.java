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

    // validate that the list of members is not empty and contains at least 2 members
    private List<UUID> members;
}
