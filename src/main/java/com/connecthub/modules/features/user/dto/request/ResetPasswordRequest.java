package com.connecthub.modules.features.user.dto.request;

import com.connecthub.modules.features.user.validation.annotation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    @NotBlank(message = "error.email.required")
    @Email(message = "error.email.invalid")
    private String email;

    @NotBlank(message = "error.otp.required")
    @Pattern(regexp = "\\d{6}", message = "error.otp.invalid")
    private String otp;

    @ValidPassword
    private String newPassword;
}
