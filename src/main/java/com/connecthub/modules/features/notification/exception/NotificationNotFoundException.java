package com.connecthub.modules.features.notification.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

public class NotificationNotFoundException extends AppException {
    public NotificationNotFoundException() {
        super(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
