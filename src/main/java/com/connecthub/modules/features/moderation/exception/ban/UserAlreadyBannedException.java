package com.connecthub.modules.features.moderation.exception.ban;

import com.connecthub.modules.features.moderation.exception.ModerationErrorCode;
import com.connecthub.modules.features.moderation.exception.ModerationException;

public class UserAlreadyBannedException extends ModerationException {
    public UserAlreadyBannedException() {
        super(ModerationErrorCode.USER_ALREADY_BANNED);
    }
}
