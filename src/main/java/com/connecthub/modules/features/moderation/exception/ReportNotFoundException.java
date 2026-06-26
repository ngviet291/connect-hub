package com.connecthub.modules.features.moderation.exception;

public class ReportNotFoundException extends ModerationException {
    public ReportNotFoundException() {
        super(ModerationErrorCode.REPORT_NOT_FOUND);
    }
}
