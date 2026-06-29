package com.connecthub.modules.features.chat.dto.request;

import com.connecthub.common.validation.anotation.ValidImageFile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "New name for the conversation", example = "Project Alpha Team")
    @Size(min = 1, max = 100, message = "error.conversation.name_length")
    private String name;

    @Schema(description = "New avatar image for the conversation")
    @ValidImageFile
    private MultipartFile avatar;
}