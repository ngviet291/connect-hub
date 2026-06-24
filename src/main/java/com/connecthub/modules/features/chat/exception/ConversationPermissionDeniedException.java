package com.connecthub.modules.features.chat.exception;

public class ConversationPermissionDeniedException extends ChatException {
    public ConversationPermissionDeniedException() {
        super(ChatErrorCode.CONVERSATION_PERMISSION_DENIED);
    }
}
