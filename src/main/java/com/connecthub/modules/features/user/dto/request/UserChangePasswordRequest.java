package com.connecthub.modules.features.user.dto.request;

import com.connecthub.modules.features.user.validation.annotation.ValidPassword;
import com.connecthub.modules.features.user.validation.annotation.ValidUsername;
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
    @ValidUsername
    private String username;

    @ValidPassword
    @Schema(description = "Current password of the user", example = "currentPassword123")
    private String oldPassword;

    @ValidPassword
    @Schema(description = "New password for the user", example = "newPassword123")
    private String newPassword;
}
