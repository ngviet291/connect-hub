package com.connecthub.modules.features.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request object for user logout, containing access token and refresh token")
public class LogoutRequest {
    @Schema(description = "The access token to be invalidated during logout", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "error.accesstoken.required")
    private String accessToken;
    @Schema(description = "The refresh token to be invalidated during logout", example = "d4519dac-ae2c-46d3-a432-ede6af745ef4")
    @NotBlank(message = "error.refreshtoken.required")
    private String refreshToken;
}