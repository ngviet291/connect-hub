package com.connecthub.modules.features.moderation.exception;

public class DuplicatePendingReportException extends ModerationException {
    public DuplicatePendingReportException() {
        super(ModerationErrorCode.DUPLICATE_PENDING_REPORT);
    }
}