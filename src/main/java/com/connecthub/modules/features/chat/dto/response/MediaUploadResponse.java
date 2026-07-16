package com.connecthub.modules.features.chat.dto.response;

import com.connecthub.modules.features.post.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MediaUploadResponse {
    private String url;
    private MediaType type;
    private String fileName;
    private Long size;
}