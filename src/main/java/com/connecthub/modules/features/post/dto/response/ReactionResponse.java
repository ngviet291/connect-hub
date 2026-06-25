package com.connecthub.modules.features.post.dto.response;

import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionResponse {

    private UUID id;
    private UserSummaryResponse user;
    private ReactionType type;
    private LocalDateTime createdAt;
}