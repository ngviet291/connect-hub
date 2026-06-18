package com.connecthub.modules.features.post.dto.request;

import com.connecthub.modules.features.post.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "The ID of the post", example = "019ed9d6-65e9-7267-b396-7ac0ad80ded8")
    private UUID id;
    @Schema(description = "The content of the post", example = "Hello, world!")
    private String content;
    @Schema(description = "The ID of the parent post if this is a comment", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID parentPostId;
    @Schema(description = "The ID of the quoted post if this is a quote", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID quotePostId;
    @Schema(description = "The visibility of the post", example = "PUBLIC")
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;
    @Schema(description = "The list of media IDs attached to the post", example = "[\"123e4567-e89b-12d3-a456-426614174000\"]")
    private List<UUID> mediaIds;
    @Schema(description = "The list of hashtags included in the post", example = "[\"#fun\", \"#connecthub\"]")
    private List<String> hashtags;
    @Schema(description = "The list of user IDs mentioned in the post", example = "[\"123e4567-e89b-12d3-a456-426614174000\"]")
    private List<UUID> mentionUserIds;
}