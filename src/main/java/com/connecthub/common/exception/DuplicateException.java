package com.connecthub.common.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class DuplicateException extends ParameterizedException {

    public DuplicateException(ErrorCode errorCode, String keyAttribute, String attributeValue) {
        super(errorCode, Map.of(keyAttribute, attributeValue));
    }
}
