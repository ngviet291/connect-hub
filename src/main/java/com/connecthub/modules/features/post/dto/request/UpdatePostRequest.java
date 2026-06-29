package com.connecthub.modules.features.post.dto.request;

import com.connecthub.modules.features.post.enums.Visibility;
import com.connecthub.modules.features.user.validation.annotation.ValidUsername;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePostRequest {
    @Schema(description = "Nội dung bài đăng", example = "Hello @john, check out #spring!")
    @Size(max = 2000, message = "error.post.content_length")
    private String content;

    @Schema(description = "The visibility of the post", example = "PUBLIC")
    @NotNull(message = "error.post.visibility_required")
    private Visibility visibility;

    @Schema(description = "Danh sách hashtag (không cần dấu #)", example = "[\"spring\", \"connecthub\"]")
    @Size(max = 20, message = "error.post.hashtags_limit")
    private List<@Pattern(regexp = "^[a-zA-Z0-9_]{1,30}$", message = "error.post.hashtag_invalid") String> hashtags;

    @Size(max = 20, message = "error.post.mentions_limit")
    private List<@ValidUsername String> mentionUsernames;
}
