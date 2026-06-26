package com.connecthub.modules.features.moderation.exception.ban;

import com.connecthub.common.exception.BaseErrorCode;
import com.connecthub.modules.features.moderation.exception.ModerationErrorCode;
import com.connecthub.modules.features.moderation.exception.ModerationException;

public class BanAlreadyInactiveException extends ModerationException {

    public BanAlreadyInactiveException() {
        super(ModerationErrorCode.BAN_ALREADY_INACTIVE);
    }
}
