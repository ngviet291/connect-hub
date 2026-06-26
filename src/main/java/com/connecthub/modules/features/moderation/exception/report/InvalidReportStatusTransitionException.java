package com.connecthub.modules.features.moderation.exception.report;

import com.connecthub.common.exception.ParameterizedException;
import com.connecthub.modules.features.moderation.exception.ModerationErrorCode;

import java.util.Map;

public class InvalidReportStatusTransitionException extends ParameterizedException {


    private static final String OLD_STATUS_PARAM = "oldStatus";
    private static final String NEW_STATUS_PARAM = "newStatus";


    public InvalidReportStatusTransitionException(String oldStatus, String newStatus) {
        super(ModerationErrorCode.INVALID_REPORT_STATUS_TRANSITION, Map.of(
                OLD_STATUS_PARAM, oldStatus,
                NEW_STATUS_PARAM, newStatus
        ));
    }
}
