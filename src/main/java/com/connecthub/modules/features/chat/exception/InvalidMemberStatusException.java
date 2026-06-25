package com.connecthub.modules.features.chat.exception;

import com.connecthub.common.exception.ParameterizedException;

import java.util.Map;

public class InvalidMemberStatusException extends ParameterizedException {
    private static final String STATUS_PARAM = "status";
    public InvalidMemberStatusException(String status) {
        super(ChatErrorCode.INVALID_MEMBER_STATUS, Map.of(STATUS_PARAM, status));
    }
}
