package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum UserErrorCode implements BaseErrorCode {
    USER_ALREADY_FOLLOWED("error.user.already_followed", HttpStatus.CONFLICT),

    ;

    private final String message;
    private final HttpStatusCode statusCode;

    UserErrorCode(String message, HttpStatusCode statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return name();
    }
}