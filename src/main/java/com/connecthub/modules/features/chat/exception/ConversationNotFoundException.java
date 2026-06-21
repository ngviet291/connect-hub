package com.connecthub.modules.features.chat.exception;

public class ConversationNotFoundException extends ChatException {
    public ConversationNotFoundException() {
        super(ChatErrorCode.CONVERSATION_NOT_FOUND);
    }
}
