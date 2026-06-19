package com.connecthub.common.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION( "Uncategorized Exception", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR( "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST( "Bad Request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED( "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN( "Forbidden", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED( "Unauthenticated", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED( "Account Locked", HttpStatus.LOCKED),
    USER_NOT_FOUND( "User Not Found", HttpStatus.NOT_FOUND),
    INVALID_PARAMETER_TYPE( "Invalid parameter type", HttpStatus.BAD_REQUEST),
    INVALID_DATE_FORMAT( "Invalid date format", HttpStatus.BAD_REQUEST),
    INVALID_NUMBER_FORMAT( "Invalid number format", HttpStatus.BAD_REQUEST),
    GENERATE_TOKEN_FAILED( "Failed to generate token", HttpStatus.INTERNAL_SERVER_ERROR),
    INTROSPECT_FAILED( "Failed to introspect token", HttpStatus.INTERNAL_SERVER_ERROR),
    DUPLICATE_USERNAME( "Username {username} already exists", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL( "Email {email} already exists", HttpStatus.CONFLICT),
    DUPLICATE_PHONE( "Phone number {phone} already exists", HttpStatus.CONFLICT),
    BAD_CREDENTIALS( "Bad credentials", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED( "Token expired", HttpStatus.UNAUTHORIZED),
    CONFLICT_USER( "Conflict User", HttpStatus.CONFLICT),

    NOTIFICATION_NOT_FOUND( "Notification not found", HttpStatus.NOT_FOUND)



    ;


    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(String message, HttpStatusCode statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
