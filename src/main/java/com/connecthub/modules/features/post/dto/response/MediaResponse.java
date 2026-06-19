package com.connecthub.modules.features.post.dto.response;

import com.connecthub.modules.features.post.enums.MediaType;
import lombok.Builder;
import lombok.*;

import java.math.BigInteger;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponse {

    private UUID id;
    private String url;
    private MediaType type;
    private BigInteger size;
}