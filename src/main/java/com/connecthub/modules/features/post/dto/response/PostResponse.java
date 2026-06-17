package com.connecthub.modules.features.post.dto.response;

import com.connecthub.modules.features.post.enums.Visibility;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    private UUID id;

    private UserSummaryResponse author;

    private String content;

    private Visibility visibility;

    private UUID parentPostId;

    private UUID quotePostId;

    private List<MediaResponse> media;

    private int reactionCount;

    private int commentCount;

    private int repostCount;

    private int bookmarkCount;

    private int viewCount;

    private boolean reacted;

    private boolean bookmarked;

    private boolean reposted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}