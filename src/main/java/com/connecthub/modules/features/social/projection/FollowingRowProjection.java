package com.connecthub.modules.features.social.projection;

import java.util.UUID;

public interface FollowingRowProjection {
    UUID getFollowId();
    UUID getId();
    String getUsername();
    String getFullName();
    String getAvatarUrl();
    Boolean getIsFollowing();
}