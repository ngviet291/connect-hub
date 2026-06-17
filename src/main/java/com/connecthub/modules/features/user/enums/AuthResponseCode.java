package com.connecthub.modules.features.user.enums;

import lombok.Getter;

@Getter
public enum AuthResponseCode {
    REGISTER_SUCCESS("Registration successful", 1000),
    LOGIN_SUCCESS("Login successful", 1001),
    LOGOUT_SUCCESS("Logout successful", 1002),
    TOKEN_REFRESH_SUCCESS("Token refresh successful", 1003),
    PASSWORD_CHANGE_SUCCESS("Password change successful", 1004),
    INTROSPECT_SUCCESS("Token introspection successful", 1005);

    private final String message;
    private final int code;

    AuthResponseCode(String message, int code) {
        this.message = message;
        this.code = code;
    }


}
