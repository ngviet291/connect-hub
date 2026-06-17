package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.DuplicateException;
import com.connecthub.common.exception.ErrorCode;

public class DuplicatePhoneNumberException extends DuplicateException {
    public DuplicatePhoneNumberException(String keyAttribute, String attributeValue) {
        super(ErrorCode.DUPLICATE_PHONE, keyAttribute, attributeValue);
    }
}
