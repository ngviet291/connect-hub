package com.connecthub.modules.features.post.dto.response;

import com.connecthub.modules.features.post.enums.ReactionType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionCountResponse {

    private ReactionType type;
    private long count;
}