package com.connecthub.modules.features.moderation.exception.report;

import com.connecthub.modules.features.moderation.exception.ModerationErrorCode;
import com.connecthub.modules.features.moderation.exception.ModerationException;

public class ReportNotFoundException extends ModerationException {
    public ReportNotFoundException() {
        super(ModerationErrorCode.REPORT_NOT_FOUND);
    }
}
