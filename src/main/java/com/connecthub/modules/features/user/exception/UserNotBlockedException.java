package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

public class UserNotBlockedException extends AppException {
    public UserNotBlockedException() {
        super(ErrorCode.USER_NOT_BLOCKED);
    }
}
