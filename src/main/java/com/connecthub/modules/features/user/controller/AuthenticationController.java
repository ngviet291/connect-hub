
package com.connecthub.modules.features.user.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.modules.features.user.dto.request.*;
import com.connecthub.modules.features.user.dto.response.AuthenticateResponse;
import com.connecthub.modules.features.user.dto.response.IntrospectResponse;
import com.connecthub.modules.features.user.dto.response.UserChangePasswordResponse;
import com.connecthub.modules.features.user.dto.response.UserResponse;
import com.connecthub.modules.features.user.enums.AuthResponseCode;
import com.connecthub.modules.features.user.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {


    private final AuthenticationService authenticationService;

    // Response status is 201 Created because we are creating a new user when registering
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = authenticationService.register(request);
        return ApiResponse.<UserResponse>builder()
                .code(AuthResponseCode.REGISTER_SUCCESS.getCode())
                .message(AuthResponseCode.REGISTER_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthenticateResponse> login(@Valid @RequestBody AuthenticateRequest request) {
        AuthenticateResponse response = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticateResponse>builder()
                .code(AuthResponseCode.LOGIN_SUCCESS.getCode())
                .message(AuthResponseCode.LOGIN_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@Valid @RequestBody IntrospectRequest request) {
        IntrospectResponse response = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .code(AuthResponseCode.INTROSPECT_SUCCESS.getCode())
                .message(AuthResponseCode.INTROSPECT_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<AuthenticateResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthenticateResponse response = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticateResponse>builder()
                .code(AuthResponseCode.TOKEN_REFRESH_SUCCESS.getCode())
                .message(AuthResponseCode.TOKEN_REFRESH_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PutMapping("/change-password")
    public ApiResponse<UserChangePasswordResponse> changePassword(@RequestBody UserChangePasswordRequest request) {
        UserChangePasswordResponse response = authenticationService.changePassword(request);
        return ApiResponse.<UserChangePasswordResponse>builder()
                .code(AuthResponseCode.PASSWORD_CHANGE_SUCCESS.getCode())
                .message(AuthResponseCode.PASSWORD_CHANGE_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request) {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .code(AuthResponseCode.LOGOUT_SUCCESS.getCode())
                .message(AuthResponseCode.LOGOUT_SUCCESS.getMessage())
                .build();
    }
}
