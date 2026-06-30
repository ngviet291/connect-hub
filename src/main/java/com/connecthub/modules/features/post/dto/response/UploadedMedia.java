package com.connecthub.modules.features.post.dto.response;

import com.connecthub.modules.features.post.enums.MediaType;

public record UploadedMedia(
        String url,
        String publicId,
        MediaType type,
        long size
) {}