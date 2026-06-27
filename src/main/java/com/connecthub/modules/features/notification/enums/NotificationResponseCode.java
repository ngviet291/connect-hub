package com.connecthub.modules.features.notification.enums;

import lombok.Getter;

@Getter
public enum NotificationResponseCode {
    READ_NOTIFICATION(
            8000,
            "success.notification.read"
    ),

    READ_ALL_NOTIFICATION(
            8001,
            "success.notification.read_all"
    ),

    COUNT_UNREAD(
            8002,
            "success.notification.count_unread"
    ),

    GET_NOTIFICATION(
            8003,
            "success.notification.get"
    );

    private final int code;
    private final String message;
    NotificationResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}