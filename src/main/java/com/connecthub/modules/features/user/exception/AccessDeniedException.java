package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

public class AccessDeniedException extends AppException {
    public AccessDeniedException() {
        super(ErrorCode.ACCESS_DENIED);
    }
}
