package com.connecthub.modules.features.chat.exception;

import com.connecthub.common.exception.AppException;

public class SenderNotConversationMemberException extends ChatException {
    public SenderNotConversationMemberException() {
        super(ChatErrorCode.SENDER_NOT_CONVERSATION_MEMBER);
    }
}