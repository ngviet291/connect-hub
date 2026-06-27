package com.connecthub.modules.features.post.dto.request;

import com.connecthub.modules.features.post.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePostRequest {
    @Schema(description = "The content of the post", example = "Hello, world!")
    private String content;

    @Schema(description = "The visibility of the post", example = "PUBLIC")
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Schema(description = "Danh sách hashtag mới (thay thế toàn bộ hashtag cũ, không cần dấu #)", example = "[\"spring\", \"java\"]")
    private List<String> hashtags;

    @Schema(description = "Danh sách username mention mới (thay thế toàn bộ mention cũ, không cần dấu @)", example = "[\"john_doe\"]")
    private List<String> mentionUsernames;
}
