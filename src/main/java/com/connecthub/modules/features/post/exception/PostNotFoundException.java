package com.connecthub.modules.features.post.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;
import com.connecthub.common.exception.ParameterizedException;

import java.util.Map;

public class PostNotFoundException extends ParameterizedException {

    private static final String REASON = "reason";
    public PostNotFoundException() {
        super(ErrorCode.POST_NOT_FOUND, Map.of());
    }
    public PostNotFoundException(String reason) {
        super(
                ErrorCode.POST_NOT_FOUND,
                Map.of(REASON, reason)
        );
    }
}