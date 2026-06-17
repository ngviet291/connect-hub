package com.connecthub.modules.features.notification.enums;

import lombok.Getter;

@Getter
public enum NotificationResponseCode {
    READ_NOTIFICATION(
            8000,
            "Notification marked as read successfully"
    ),

    READ_ALL_NOTIFICATION(
            8001,
            "All notifications marked as read successfully"
    ),

    COUNT_UNREAD(
            8002,
            "Unread notification count retrieved successfully"
    ),

    GET_NOTIFICATION(
            8003,
            "Notifications retrieved successfully"
    );

    private final int code;
    private final String message;
    NotificationResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
