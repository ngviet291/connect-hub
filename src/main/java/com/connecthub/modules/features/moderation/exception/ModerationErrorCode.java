package com.connecthub.modules.features.moderation.exception;

import com.connecthub.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ModerationErrorCode implements BaseErrorCode {

    DUPLICATE_PENDING_REPORT("error.moderation.duplicate_pending_report", HttpStatus.CONFLICT),
    REPORT_NOT_FOUND("error.moderation.report_not_found", HttpStatus.NOT_FOUND),
    INVALID_REPORT_STATUS_TRANSITION("error.moderation.invalid_report_status_transition", HttpStatus.CONFLICT),
    REPORT_STATUS_NOT_EXIST("error.moderation.report_status_not_exist", HttpStatus.BAD_REQUEST),
    USER_ALREADY_BANNED("error.moderation.user_already_banned", HttpStatus.CONFLICT),
    BAN_ALREADY_INACTIVE("error.moderation.ban_already_inactive", HttpStatus.CONFLICT),
    BAN_NOT_FOUND("error.moderation.ban_not_found", HttpStatus.NOT_FOUND);

    private final String message;
    private final HttpStatusCode statusCode;

    ModerationErrorCode(String message, HttpStatusCode statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}