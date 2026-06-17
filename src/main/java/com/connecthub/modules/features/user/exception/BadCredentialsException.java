package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

public class BadCredentialsException extends AppException {
    public BadCredentialsException() {
        super(ErrorCode.BAD_CREDENTIALS);
    }
}
