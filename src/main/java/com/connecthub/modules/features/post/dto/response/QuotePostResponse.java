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
public class QuotePostResponse {

    private UUID id;
    private UserSummaryResponse author;
    private String content;
    private Visibility visibility;
    private List<MediaResponse> media;
    private LocalDateTime createdAt;
}