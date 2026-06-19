package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

public class ConflictUserException extends AppException {
    public ConflictUserException( ) {
        super(ErrorCode.CONFLICT_USER);
    }
}
