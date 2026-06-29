package com.connecthub.modules.features.user.dto.request;


import com.connecthub.modules.features.user.validation.annotation.ValidPassword;
import com.connecthub.modules.features.user.validation.annotation.ValidUsername;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "User creation request")
public class UserCreateRequest {
    @Schema(description = "Unique username for the user", example = "johndoe")
    @ValidUsername
    private String username;

    @Schema(description = "Password for the user account", example = "Test12345@")
    @ValidPassword
    private String password;

    @Schema(description = "Email address for the user", example = "john.doe@example.com")
    private String email;
    @Schema(description = "Full name of the user", example = "John Doe")
    private String fullName;
    @Schema(description = "Phone number for the user", example = "+1234567890")
    private String phoneNumber;
}
