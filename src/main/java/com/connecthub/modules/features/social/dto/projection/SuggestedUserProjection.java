package com.connecthub.modules.features.social.dto.projection;

import com.connecthub.modules.features.user.entity.User;

import java.util.UUID;

public interface SuggestedUserProjection {
    UUID getId();
    String getUsername();
    String getFullName();
    String getAvatarUrl();
    Long getMutualCount();
}