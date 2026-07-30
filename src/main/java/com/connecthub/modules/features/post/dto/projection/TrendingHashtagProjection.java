package com.connecthub.modules.features.post.dto.projection;

import java.util.UUID;

public interface TrendingHashtagProjection {
    UUID getId();
    String getName();
    long getPostCount();
}