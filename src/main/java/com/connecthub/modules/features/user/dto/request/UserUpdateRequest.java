package com.connecthub.modules.features.user.dto.request;

import com.connecthub.modules.features.user.validation.annotation.ValidPhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request object for updating user profile")
public class UserUpdateRequest {

    @Schema(description = "Full name of the user", example = "John Doe")
    @Size(max = 100, message = "error.fullname.length")
    private String fullName;

    @Schema(description = "Phone number of the user", example = "+1234567890")
    @ValidPhoneNumber
    private String phoneNumber;

    @Schema(description = "Avatar URL of the user", example = "https://example.com/avatar.png")
    @URL(message = "error.avatarurl.invalid")
    private String avatarUrl;

    @Schema(description = "User biography", example = "Backend engineer and coffee lover")
    @Size(max = 500, message = "error.bio.length")
    private String bio;
}

