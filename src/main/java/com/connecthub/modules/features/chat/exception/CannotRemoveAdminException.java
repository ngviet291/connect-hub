package com.connecthub.modules.features.chat.exception;


public class CannotRemoveAdminException extends ChatException {
    public CannotRemoveAdminException() {
        super(ChatErrorCode.CANNOT_REMOVE_ADMIN);
    }
}
