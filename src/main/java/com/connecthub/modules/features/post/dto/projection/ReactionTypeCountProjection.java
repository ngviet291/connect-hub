package com.connecthub.modules.features.post.dto.projection;

import com.connecthub.modules.features.post.enums.ReactionType;

public interface ReactionTypeCountProjection {
    ReactionType getType();
    long getCount();
}
