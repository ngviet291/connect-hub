package com.connecthub.modules.features.chat.exception;

public class ConversationMemberNotFoundException extends ChatException {
    public ConversationMemberNotFoundException() {
        super(ChatErrorCode.CONVERSATION_MEMBER_NOT_FOUND);
    }
}
