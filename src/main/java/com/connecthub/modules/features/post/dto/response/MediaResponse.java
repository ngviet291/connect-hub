package com.connecthub.modules.features.post.dto.response;

import com.connecthub.modules.features.post.enums.MediaType;
import lombok.Builder;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaResponse {
    private UUID mediaId;
    private String url;
    private String fileName;     // "report.pdf"
    private Long fileSize;       // bytes
    private String mimeType;     // "application/pdf"
    private MediaType type;      // IMAGE | VIDEO | FILE
}