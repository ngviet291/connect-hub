package com.connecthub.modules.features.moderation.dto.response;

import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PostSummaryResponse {
    private String id;
    private String content;
    private UserSummaryResponse author;
}
