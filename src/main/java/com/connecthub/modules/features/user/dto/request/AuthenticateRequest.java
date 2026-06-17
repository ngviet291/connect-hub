package com.connecthub.modules.features.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private String username;
    @Schema(description = "User's password", example = "password123")
    private String password;
}
