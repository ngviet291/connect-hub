package com.connecthub.modules.features.post.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;
import com.connecthub.common.exception.ParameterizedException;

import java.util.Map;

public class PostAccessDeniedException extends ParameterizedException {
    private static final String ACTION = "action";
    public PostAccessDeniedException(String action) {
        super(
                ErrorCode.POST_ACCESS_DENIED,
                Map.of(ACTION, action)
        );;
    }
}