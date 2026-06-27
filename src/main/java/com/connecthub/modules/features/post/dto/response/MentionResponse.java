package com.connecthub.modules.features.post.dto.response;

import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentionResponse {
    private UUID id;
    private UserSummaryResponse user;
    private LocalDateTime createdAt;
}
