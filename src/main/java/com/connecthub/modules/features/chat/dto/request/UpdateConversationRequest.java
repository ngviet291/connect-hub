package com.connecthub.modules.features.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Update name and avatar for a conversation. If the conversation is a group, the name and avatar will be updated for all members.")

public class UpdateConversationRequest {
    private String name;
    private MultipartFile avatar;
}
