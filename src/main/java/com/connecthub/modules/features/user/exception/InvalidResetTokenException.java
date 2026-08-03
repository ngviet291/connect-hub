package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

public class InvalidResetTokenException extends AppException {
    public InvalidResetTokenException() {
        super(ErrorCode.INVALID_RESET_TOKEN);
    }
}
