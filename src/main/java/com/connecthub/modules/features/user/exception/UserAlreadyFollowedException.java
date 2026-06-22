package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.AppException;

public class UserAlreadyFollowedException extends AppException {
    public UserAlreadyFollowedException() {
        super(UserErrorCode.USER_ALREADY_FOLLOWED);
    }
}
