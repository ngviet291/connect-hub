package com.connecthub.modules.features.post.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

public class PostNotFoundException extends AppException {
    public PostNotFoundException() {
        super(ErrorCode.POST_NOT_FOUND);
    }
    public PostNotFoundException(String message) {
        super(message);
    }
}