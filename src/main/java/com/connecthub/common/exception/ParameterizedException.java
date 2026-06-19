package com.connecthub.common.exception;

import lombok.Getter;

import java.util.Map;

public class ParameterizedException extends AppException {

    @Getter
    private final Map<String, Object> parameters;


    public ParameterizedException(ErrorCode errorCode, Map<String, Object> parameters) {
        super(errorCode);
        this.parameters = parameters;
    }
}
