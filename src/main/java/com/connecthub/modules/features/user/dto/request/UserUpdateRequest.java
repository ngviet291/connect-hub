package com.connecthub.modules.features.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request object for updating user profile")
public class UserUpdateRequest {

    @Schema(description = "Full name of the user", example = "John Doe")
    private String fullName;

    @Schema(description = "Phone number of the user", example = "+1234567890")
    private String phoneNumber;

    @Schema(description = "Avatar URL of the user", example = "https://example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "User biography", example = "Backend engineer and coffee lover")
    private String bio;
}

