package com.connecthub.modules.features.post.exception;

import com.connecthub.common.exception.ErrorCode;
import com.connecthub.common.exception.ParameterizedException;

import java.util.Map;
import java.util.UUID;

public class MentionedUserNotFoundException extends ParameterizedException {

    private static final String USER_ID = "userId";

    public MentionedUserNotFoundException(UUID userId) {
        super(
                ErrorCode.MENTIONED_USER_NOT_FOUND,
                Map.of(USER_ID, userId)
        );
    }
}