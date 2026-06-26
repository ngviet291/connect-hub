package com.connecthub.modules.features.moderation.exception;

import com.connecthub.common.exception.ParameterizedException;

import java.util.Map;

public class InvalidReportStatus extends ParameterizedException {
    private static final String PARAM = "status";
    public InvalidReportStatus(String status) {
        super(ModerationErrorCode.REPORT_STATUS_NOT_EXIST, Map.of(PARAM, status));
    }
}
