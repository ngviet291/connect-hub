package com.connecthub.modules.features.user.dto.request;

import com.connecthub.modules.features.user.validation.annotation.ValidPassword;
import com.connecthub.modules.features.user.validation.annotation.ValidUsername;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request object for user authentication")
public class AuthenticateRequest {
    @Schema(description = "User's username", example = "johndoe")
    @ValidUsername
    private String username;

    @Schema(description = "User's password", example = "password123")
    @ValidPassword
    private String password;
}
