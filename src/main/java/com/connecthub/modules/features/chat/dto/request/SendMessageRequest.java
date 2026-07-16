package com.connecthub.modules.features.chat.dto.request;

import com.connecthub.common.validation.anotation.RequiredUUID;
import com.connecthub.modules.features.chat.validation.annotation.ValidSendMessageTarget;
import com.connecthub.modules.features.post.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@ValidSendMessageTarget
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SendMessageRequest {

    @RequiredUUID(nullable = true)
    private UUID conversationId;

    @RequiredUUID(nullable = true)
    private UUID recipientId;

    @Size(max = 5000, message = "error.message.content_length")
    private String content;

    @RequiredUUID(nullable = true)
    private UUID replyToMessageId;

    @Size(max = 10, message = "error.message.media_limit")
    private List<MediaRequest> media;

    @Data
    public static class MediaRequest {
        @NotBlank(message = "error.message.media_url_required")
        private String url;

        @NotNull(message = "error.message.media_type_required")
        private MediaType type;

        private String fileName;

        private String publicId;

        @NotNull(message = "error.message.media_size_required")
        private Long size;
    }
}