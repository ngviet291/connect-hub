package com.connecthub.modules.features.chat.exception;


public class InvalidChatRequestException extends ChatException {
    public InvalidChatRequestException() {
        super(ChatErrorCode.INVALID_CHAT_REQUEST);
    }
}
