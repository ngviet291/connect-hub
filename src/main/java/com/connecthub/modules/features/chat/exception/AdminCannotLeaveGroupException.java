package com.connecthub.modules.features.chat.exception;

public class AdminCannotLeaveGroupException extends ChatException {
    public AdminCannotLeaveGroupException() {
        super(ChatErrorCode.ADMIN_CANNOT_LEAVE_GROUP);
    }
}
