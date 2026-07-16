package com.connecthub.modules.features.chat.exception;

public class MediaLimitExceededException extends ChatException {
    public MediaLimitExceededException() {
        super(ChatErrorCode.MEDIA_LIMIT_EXCEEDED);
    }
}