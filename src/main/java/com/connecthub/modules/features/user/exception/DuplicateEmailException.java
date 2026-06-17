package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.DuplicateException;
import com.connecthub.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicateEmailException extends DuplicateException {



    public DuplicateEmailException(String keyAttribute, String attributeValue) {
        super(ErrorCode.DUPLICATE_EMAIL, keyAttribute, attributeValue);
    }

}
