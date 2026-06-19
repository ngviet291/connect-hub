package com.connecthub.common.dto.response;

import com.connecthub.common.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UploadMediaResponse {
    private String publicId;
    private String url;
    private MediaType  mediaType;

}