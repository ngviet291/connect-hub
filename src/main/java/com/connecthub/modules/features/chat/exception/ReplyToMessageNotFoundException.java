package com.connecthub.modules.features.chat.exception;

import com.connecthub.common.exception.BaseErrorCode;
import com.connecthub.common.exception.ErrorCode;
import com.connecthub.common.exception.ParameterizedException;

import java.util.Map;

public class ReplyToMessageNotFoundException extends ParameterizedException {
    private static final String MESSAGE_ID_PARAM = "messageId";
    public ReplyToMessageNotFoundException(String messageId) {
        super(ChatErrorCode.REPLY_TO_MESSAGE_NOT_FOUND, Map.of(MESSAGE_ID_PARAM, messageId));
    }
}
