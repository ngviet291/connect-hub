package com.connecthub.modules.features.moderation.dto.request.ban;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request to unban a user")
public class UnbanRequest {
    @Schema(description = "The reason for unbanning the user", example = "The user has shown good behavior and is allowed to return.")
    private String unbanReason;

}
