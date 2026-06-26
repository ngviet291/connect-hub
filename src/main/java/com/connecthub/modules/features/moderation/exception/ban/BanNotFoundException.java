package com.connecthub.modules.features.moderation.exception.ban;

import com.connecthub.common.exception.ParameterizedException;
import com.connecthub.modules.features.moderation.exception.ModerationErrorCode;

import java.util.Map;

public class BanNotFoundException extends ParameterizedException {
    private static final String PARAM_BAN_ID = "banId";

    public BanNotFoundException(String banId) {
        super(ModerationErrorCode.BAN_NOT_FOUND, Map.of(PARAM_BAN_ID, banId));
    }
}
