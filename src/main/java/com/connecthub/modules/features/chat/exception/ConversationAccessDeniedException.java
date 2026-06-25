package com.connecthub.modules.features.chat.exception;

public class ConversationAccessDeniedException extends ChatException {

    public ConversationAccessDeniedException() {
        super(ChatErrorCode.CONVERSATION_ACCESS_DENIED);
    }
}
