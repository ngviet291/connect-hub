package com.connecthub.modules.features.chat.dto.request;

import com.connecthub.common.validation.anotation.RequiredUUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class CreateGroupConversationRequest {

    @Size(min = 1, max = 100, message = "error.conversation.name_length")
    private String name;

    @NotEmpty(message = "error.conversation.members_required")
    @Size(min = 2, message = "error.conversation.members_min_size")
    @RequiredUUID
    private List<UUID> members;
}
