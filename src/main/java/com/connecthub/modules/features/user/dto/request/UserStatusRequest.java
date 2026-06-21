package com.connecthub.modules.features.user.dto.request;

import com.connecthub.modules.features.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request object for changing user account status")
public class UserStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New account status", example = "ACTIVE")
    private UserStatus status;
}

