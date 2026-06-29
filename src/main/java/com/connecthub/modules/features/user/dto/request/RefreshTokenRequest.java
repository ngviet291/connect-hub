package com.connecthub.modules.features.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request object for refreshing access token using refresh token")
public class RefreshTokenRequest {
    @Schema(description = "The refresh token to use for obtaining a new access token", example = "d4519dac-ae2c-46d3-a432-ede6af745ef4")

    @NotBlank(message = "error.refreshtoken.required")
    private String refreshToken;
}
