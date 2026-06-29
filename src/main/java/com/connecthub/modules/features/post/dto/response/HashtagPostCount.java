package com.connecthub.modules.features.post.dto.response;

import java.util.UUID;

public interface HashtagPostCount {
    UUID getHashtagId();
    Long getPostCount();
}