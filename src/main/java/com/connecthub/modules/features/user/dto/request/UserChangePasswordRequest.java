package com.connecthub.modules.features.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request object for changing user password")
public class UserChangePasswordRequest {

    @Schema(description = "Username of the user", example = "john.doe")
    @NotBlank(message = "Username is required")
    private String username;

    @Schema(description = "Current password of the user", example = "currentPassword123")
    private String oldPassword;

    @Schema(description = "New password for the user", example = "newPassword123")
    private String newPassword;
}
