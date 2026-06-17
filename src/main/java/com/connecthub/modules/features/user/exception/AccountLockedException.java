package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

public class AccountLockedException extends AppException {
    public AccountLockedException() {
        super(ErrorCode.ACCOUNT_LOCKED);
    }
}
