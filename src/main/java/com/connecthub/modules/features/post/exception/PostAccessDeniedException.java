package com.connecthub.modules.features.post.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

public class PostAccessDeniedException extends AppException {
    public PostAccessDeniedException(String action) {
        super("Access denied to " + action + " this post");
    }
}