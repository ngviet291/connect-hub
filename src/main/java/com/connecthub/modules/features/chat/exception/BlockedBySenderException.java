package com.connecthub.modules.features.chat.exception;

public class BlockedBySenderException extends ChatException {
    public BlockedBySenderException() {
        super(ChatErrorCode.BLOCKED_BY_SENDER);
    }
}