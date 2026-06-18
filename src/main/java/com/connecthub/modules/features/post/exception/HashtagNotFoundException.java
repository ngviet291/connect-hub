package com.connecthub.modules.features.post.exception;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

public class HashtagNotFoundException extends AppException {
    public HashtagNotFoundException() {
        super(ErrorCode.HASHTAG_NOT_FOUND);
    }
}