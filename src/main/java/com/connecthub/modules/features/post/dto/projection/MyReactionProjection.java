package com.connecthub.modules.features.post.dto.projection;

import com.connecthub.modules.features.post.enums.ReactionType;

import java.util.UUID;

public interface MyReactionProjection {
    UUID getPostId();
    ReactionType getType();
}