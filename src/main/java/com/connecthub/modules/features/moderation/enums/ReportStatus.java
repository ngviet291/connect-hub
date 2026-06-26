package com.connecthub.modules.features.moderation.enums;

import com.connecthub.modules.features.moderation.exception.InvalidReportStatus;

public enum ReportStatus {
    PENDING, // đang chờ xử lý
    RESOLVED, // đã được xử lý
    REJECTED, // đã từ chối
    REVIEWING // đang được xem xét

    ;


    public static ReportStatus fromString(String status) {
        for (ReportStatus reportStatus : ReportStatus.values()) {
            if (reportStatus.name().equalsIgnoreCase(status)) {
                return reportStatus;
            }
        }
        throw new InvalidReportStatus(status);
    }
}
