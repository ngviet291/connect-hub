package com.connecthub.modules.features.user.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowResponse {
    private UUID followerId;
    private UUID followingId;
    private String message;
    private boolean success;
}

