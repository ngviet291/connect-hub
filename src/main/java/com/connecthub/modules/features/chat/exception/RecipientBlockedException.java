package com.connecthub.modules.features.chat.exception;

public class RecipientBlockedException extends ChatException {
    public RecipientBlockedException() {
        super(ChatErrorCode.RECIPIENT_BLOCKED);
    }
}