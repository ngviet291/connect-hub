package com.connecthub.modules.features.chat.exception;

import com.connecthub.common.exception.AppException;

public class ChatException extends AppException{
    public ChatException(ChatErrorCode errorCode) {
        super(errorCode);
    } 
}
