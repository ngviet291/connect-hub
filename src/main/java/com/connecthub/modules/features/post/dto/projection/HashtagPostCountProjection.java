package com.connecthub.modules.features.post.dto.projection;

import java.util.UUID;

public interface HashtagPostCountProjection {
    UUID getHashtagId();
    Long getPostCount();
}