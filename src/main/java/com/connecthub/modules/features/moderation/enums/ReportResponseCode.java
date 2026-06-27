package com.connecthub.modules.features.moderation.enums;

import lombok.Getter;

@Getter
public enum ReportResponseCode {
    CREATED(201, "success.report.created"),
    GET_REPORTS(200, "success.report.get_reports"),
    GET_REPORT(200, "success.report.get_report"),
    UPDATE_STATUS(200, "success.report.update_status"),
    GET_MY_REPORTS(200, "success.report.get_my_reports");

    private final int code;
    private final String message;

    ReportResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}