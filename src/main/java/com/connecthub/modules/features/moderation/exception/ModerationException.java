package com.connecthub.modules.features.moderation.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.BaseErrorCode;

public class ModerationException extends AppException {
    public ModerationException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
