package com.connecthub.modules.features.post.dto.request;

import com.connecthub.modules.features.post.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequest {

    private String content;

    private UUID parentPostId;

    private UUID quotePostId;

    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    private List<UUID> mediaIds;

    private List<String> hashtags;

    private List<UUID> mentionUserIds;
}