package com.connecthub.modules.features.user.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlockStatusResponse {
    private boolean isBlocked;
}
