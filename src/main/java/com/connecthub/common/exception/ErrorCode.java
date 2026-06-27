package com.connecthub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode implements BaseErrorCode {
    UNCATEGORIZED_EXCEPTION("error.uncategorized_exception", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR("error.internal_server_error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("error.bad_request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("error.unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("error.forbidden", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED("error.unauthenticated", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("error.account_locked", HttpStatus.LOCKED),
    USER_NOT_FOUND("error.user_not_found", HttpStatus.NOT_FOUND),
    INVALID_PARAMETER_TYPE("error.invalid_parameter_type", HttpStatus.BAD_REQUEST),
    INVALID_DATE_FORMAT("error.invalid_date_format", HttpStatus.BAD_REQUEST),
    INVALID_NUMBER_FORMAT("error.invalid_number_format", HttpStatus.BAD_REQUEST),
    GENERATE_TOKEN_FAILED("error.generate_token_failed", HttpStatus.INTERNAL_SERVER_ERROR),
    INTROSPECT_FAILED("error.introspect_failed", HttpStatus.INTERNAL_SERVER_ERROR),
    DUPLICATE_USERNAME("error.duplicate_username", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL("error.duplicate_email", HttpStatus.CONFLICT),
    DUPLICATE_PHONE("error.duplicate_phone", HttpStatus.CONFLICT),
    BAD_CREDENTIALS("error.bad_credentials", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("error.token_expired", HttpStatus.UNAUTHORIZED),

    NOTIFICATION_NOT_FOUND("error.notification_not_found", HttpStatus.NOT_FOUND),
    HASHTAG_NOT_FOUND("error.hashtag_not_found", HttpStatus.NOT_FOUND),
    MENTIONED_USER_NOT_FOUND("error.mentioned_user_not_found", HttpStatus.NOT_FOUND),
    POST_ACCESS_DENIED("error.post_access_denied", HttpStatus.FORBIDDEN),
    POST_NOT_FOUND("error.post_not_found", HttpStatus.NOT_FOUND),

    CONFLICT_USERNAME("error.conflict_username", HttpStatus.CONFLICT),

    UPLOAD_MEDIA_FAILED("error.upload_media_failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND("error.file_not_found", HttpStatus.NOT_FOUND),
    FILE_SIZE_EXCEEDED("error.file_size_exceeded", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE("error.invalid_file_type", HttpStatus.BAD_REQUEST),
    USER_NOT_BLOCKED("error.user_not_blocked", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(String message, HttpStatusCode statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}