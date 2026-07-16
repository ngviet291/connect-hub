package com.connecthub.modules.features.chat.exception;

public class MediaFileRequiredException extends ChatException {
    public MediaFileRequiredException() {
        super(ChatErrorCode.MEDIA_FILE_REQUIRED);
    }
}