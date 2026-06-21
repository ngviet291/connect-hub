package com.connecthub.modules.features.chat.dto.request;

import com.connecthub.modules.features.post.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SendMessageRequest {

    private UUID conversationId;
    private UUID recipientId;

    private String content;          // null nếu chỉ gửi media

    private UUID replyToMessageId;   // null nếu không reply

    private List<MediaRequest> media; // null hoặc empty nếu chỉ gửi text

    @Data
    public static class MediaRequest {
        @NotBlank
        private String url;
        @NotNull
        private MediaType type;
        private String fileName;
        private Long size;
    }
}
