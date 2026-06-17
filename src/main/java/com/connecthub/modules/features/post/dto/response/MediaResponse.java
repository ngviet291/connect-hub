package com.connecthub.modules.features.post.dto.response;

import lombok.Builder;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaResponse {

    private UUID id;

    private String url;

    private String type;
}