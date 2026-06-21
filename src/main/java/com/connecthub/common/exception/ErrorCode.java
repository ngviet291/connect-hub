package com.connecthub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode implements BaseErrorCode {
    UNCATEGORIZED_EXCEPTION("Uncategorized Exception", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("Bad Request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("Forbidden", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED("Unauthenticated", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("Account Locked", HttpStatus.LOCKED),
    USER_NOT_FOUND("User Not Found", HttpStatus.NOT_FOUND),
    INVALID_PARAMETER_TYPE("Invalid parameter type", HttpStatus.BAD_REQUEST),
    INVALID_DATE_FORMAT("Invalid date format", HttpStatus.BAD_REQUEST),
    INVALID_NUMBER_FORMAT("Invalid number format", HttpStatus.BAD_REQUEST),
    GENERATE_TOKEN_FAILED("Failed to generate token", HttpStatus.INTERNAL_SERVER_ERROR),
    INTROSPECT_FAILED("Failed to introspect token", HttpStatus.INTERNAL_SERVER_ERROR),
    DUPLICATE_USERNAME("Username {username} already exists", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL("Email {email} already exists", HttpStatus.CONFLICT),
    DUPLICATE_PHONE("Phone number {phone} already exists", HttpStatus.CONFLICT),
    BAD_CREDENTIALS("Bad credentials", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("Token expired", HttpStatus.UNAUTHORIZED),


    NOTIFICATION_NOT_FOUND("Notification not found", HttpStatus.NOT_FOUND),
    HASHTAG_NOT_FOUND("Hashtag not found", HttpStatus.NOT_FOUND),
    MENTIONED_USER_NOT_FOUND("Mentioned user not found", HttpStatus.NOT_FOUND),
    POST_ACCESS_DENIED("Access denied to the post", HttpStatus.FORBIDDEN),
    POST_NOT_FOUND("Post not found", HttpStatus.NOT_FOUND),

    CONFLICT_USERNAME( "Username already exists", HttpStatus.CONFLICT),

    UPLOAD_MEDIA_FAILED("Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND( "File not found", HttpStatus.NOT_FOUND),
    FILE_SIZE_EXCEEDED( "File size exceeded the limit",HttpStatus.BAD_REQUEST)
    ;


    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(String message, HttpStatusCode statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
