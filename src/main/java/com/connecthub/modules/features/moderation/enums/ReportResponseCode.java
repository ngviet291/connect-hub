package com.connecthub.modules.features.moderation.enums;

import lombok.Getter;

@Getter
public enum ReportResponseCode {

    CREATED(201, "Report created successfully"),
    GET_REPORTS(200, "Reports retrieved successfully"),
    GET_REPORT(200, "Report retrieved successfully"),
    UPDATE_STATUS(200, "Report status updated successfully"),
    GET_MY_REPORTS(200, "My reports retrieved successfully");

    private final int code;
    private final String message;

    ReportResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
