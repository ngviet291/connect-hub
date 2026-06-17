package com.connecthub.modules.features.user.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSummaryResponse {

    private UUID id;

    private String username;

    private String fullName;

    private String avatarUrl;

    private boolean verified;
}