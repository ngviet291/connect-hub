package com.connecthub.modules.features.user.enums;

import lombok.Getter;

@Getter
public enum AuthResponseCode {
    REGISTER_SUCCESS("success.auth.register", 1000),
    LOGIN_SUCCESS("success.auth.login", 1001),
    LOGOUT_SUCCESS("success.auth.logout", 1002),
    TOKEN_REFRESH_SUCCESS("success.auth.token_refresh", 1003),
    PASSWORD_CHANGE_SUCCESS("success.auth.password_change", 1004),
    INTROSPECT_SUCCESS("success.auth.introspect", 1005);

    private final String message;
    private final int code;

    AuthResponseCode(String message, int code) {
        this.message = message;
        this.code = code;
    }
}