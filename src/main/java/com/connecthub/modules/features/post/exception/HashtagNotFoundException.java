package com.connecthub.modules.features.post.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;
import com.connecthub.common.exception.ParameterizedException;

import java.util.Map;

public class HashtagNotFoundException extends ParameterizedException {

    private static final String HASHTAG = "hashtag";

    public HashtagNotFoundException(String hashtag) {
        super(
                ErrorCode.HASHTAG_NOT_FOUND,
                Map.of(HASHTAG, hashtag)
        );
    }
}