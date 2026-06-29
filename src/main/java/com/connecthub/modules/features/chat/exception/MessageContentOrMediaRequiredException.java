package com.connecthub.modules.features.chat.exception;

public class MessageContentOrMediaRequiredException extends ChatException {
    public MessageContentOrMediaRequiredException() {
        super(ChatErrorCode.MESSAGE_CONTENT_OR_MEDIA_REQUIRED);
    }
}
