package com.connecthub.common.exception;

import lombok.Getter;

@Getter
public class DuplicateException extends AppException {
    private final String keyAttribute;
    private final String attributeValue;

    public DuplicateException(ErrorCode errorCode, String keyAttribute, String attributeValue) {
        super(errorCode);
        this.keyAttribute = keyAttribute;
        this.attributeValue = attributeValue;
    }
}
