package com.connecthub.modules.features.post.exception;

public class MentionedUserNotFoundException extends RuntimeException {
    public MentionedUserNotFoundException(java.util.UUID userId) {
        super("Mentioned user not found: " + userId);
    }
}