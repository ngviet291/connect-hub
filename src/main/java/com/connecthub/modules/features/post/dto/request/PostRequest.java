package com.connecthub.modules.features.post.dto.request;

import com.connecthub.modules.features.post.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequest {

    @Schema(description = "Nội dung bài đăng", example = "Hello @john, check out #spring!")
    private String content;

    @Schema(description = "ID bài đăng cha nếu đây là comment")
    private UUID parentPostId;

    @Schema(description = "ID bài đăng được quote")
    private UUID quotePostId;

    @Schema(description = "Mức độ hiển thị", example = "PUBLIC")
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Schema(description = "Danh sách file ảnh/video đính kèm (multipart)")
    private List<MultipartFile> files;

    @Schema(description = "Danh sách hashtag (không cần dấu #)", example = "[\"spring\", \"connecthub\"]")
    private List<String> hashtags;

    @Schema(description = "Danh sách username cần mention (không cần dấu @)", example = "[\"john_doe\"]")
    private List<String> mentionUsernames;
}
