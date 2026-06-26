package com.connecthub.modules.features.moderation.exception;

import com.connecthub.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ModerationErrorCode implements BaseErrorCode {

    DUPLICATE_PENDING_REPORT("You have reported this object and the report is being processed.", HttpStatus.CONFLICT),
    REPORT_NOT_FOUND("Report not found.", HttpStatus.NOT_FOUND),
    INVALID_REPORT_STATUS_TRANSITION("Unable to change the report status from {oldStatus} to {newStatus}.", HttpStatus.CONFLICT),
    REPORT_STATUS_NOT_EXIST("Report status {status} does not exist.", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatusCode statusCode;

    ModerationErrorCode(String message, HttpStatusCode statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
