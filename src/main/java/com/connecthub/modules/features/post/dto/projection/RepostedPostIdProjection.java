package com.connecthub.modules.features.post.dto.projection;

import java.util.UUID;

public interface RepostedPostIdProjection {
    UUID getRepostId();
    UUID getPostId();
}
