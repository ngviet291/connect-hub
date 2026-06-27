package com.connecthub.modules.features.post.exception;

import com.connecthub.common.exception.ErrorCode;
import com.connecthub.common.exception.ParameterizedException;

import java.util.Map;
import java.util.UUID;

public class MentionedUserNotFoundException extends ParameterizedException {

//    public MentionedUserNotFoundException(UUID userId) {
//        super(ErrorCode.MENTIONED_USER_NOT_FOUND, Map.of("userId", userId));
//    }

    // Overload dùng khi tìm theo username
    public MentionedUserNotFoundException(String username) {
        super(ErrorCode.MENTIONED_USER_NOT_FOUND, Map.of("username", username));
    }
}
