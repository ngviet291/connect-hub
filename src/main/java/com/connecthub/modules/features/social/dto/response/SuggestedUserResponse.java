package com.connecthub.modules.features.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SuggestedUserResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private Long mutualCount;
}
