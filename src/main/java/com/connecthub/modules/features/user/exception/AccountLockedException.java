package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;
import com.connecthub.common.exception.ParameterizedException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
@Getter
public class AccountLockedException extends AppException {
    private final LocalDateTime lockedUntil;

    public AccountLockedException(LocalDateTime lockedUntil) {
        super(ErrorCode.ACCOUNT_LOCKED);
        this.lockedUntil = lockedUntil;
    }
}
