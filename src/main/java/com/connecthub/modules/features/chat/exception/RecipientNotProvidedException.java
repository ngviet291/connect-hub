package com.connecthub.modules.features.chat.exception;

public class RecipientNotProvidedException extends ChatException {

    public RecipientNotProvidedException() {
        super(ChatErrorCode.RECIPIENT_NOT_PROVIDED);
    }
}
