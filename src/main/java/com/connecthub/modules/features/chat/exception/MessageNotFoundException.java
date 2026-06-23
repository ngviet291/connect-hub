package com.connecthub.modules.features.chat.exception;

import com.connecthub.common.exception.ParameterizedException;

import java.util.Map;

public class MessageNotFoundException extends ParameterizedException {

    private static final String MESSAGE_PARAM = "messageId";

    public MessageNotFoundException(String messageId) {
        super(ChatErrorCode.MESSAGE_NOT_FOUND, Map.of(MESSAGE_PARAM, messageId));
    }
}
