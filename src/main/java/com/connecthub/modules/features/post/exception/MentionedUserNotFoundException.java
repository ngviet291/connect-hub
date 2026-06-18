package com.connecthub.modules.features.post.exception;

import com.connecthub.common.exception.AppException;

import java.util.UUID;

public class MentionedUserNotFoundException extends AppException {
    public MentionedUserNotFoundException(UUID userId) {
        super("Mentioned user not found: " + userId);
    }
}