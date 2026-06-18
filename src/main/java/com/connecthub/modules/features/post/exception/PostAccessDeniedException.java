package com.connecthub.modules.features.post.exception;

public class PostAccessDeniedException extends RuntimeException {
    public PostAccessDeniedException(String action) {
        super("You don't have permission to " + action + " this post");
    }
}