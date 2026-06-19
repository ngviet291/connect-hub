package com.connecthub.modules.features.chat.dto.request;

import com.connecthub.modules.features.post.enums.MediaType;
import jakarta.persistence.Entity;
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
    @NotNull
    private UUID recipientId;
    private String content;
    private List<String> media;
    private MediaType mediaType;

}
