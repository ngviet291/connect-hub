package com.connecthub.modules.features.moderation.exception.report;

import com.connecthub.modules.features.moderation.exception.ModerationErrorCode;
import com.connecthub.modules.features.moderation.exception.ModerationException;

public class DuplicatePendingReportException extends ModerationException {
    public DuplicatePendingReportException() {
        super(ModerationErrorCode.DUPLICATE_PENDING_REPORT);
    }
}