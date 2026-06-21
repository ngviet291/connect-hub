package com.connecthub.modules.features.chat.exception;

import com.connecthub.common.exception.ErrorCode;
import com.connecthub.common.exception.ParameterizedException;
import com.connecthub.modules.features.chat.enums.ConversationType;

import java.util.Map;

public class InvalidTypeConversionException extends ParameterizedException {
    private static final String CONVERSATION_TYPE_PARAM = "conversationType";
    public InvalidTypeConversionException(ConversationType conversationType) {
        super(ChatErrorCode.INVALID_TYPE_CONVERSATION, Map.of(CONVERSATION_TYPE_PARAM, conversationType.name().toLowerCase()));
    }
}
