package com.connecthub.modules.features.post.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HashtagResponse {
    private UUID id;
    private String name;
    private long postCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime createdAt;
}
