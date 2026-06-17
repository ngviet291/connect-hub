package com.connecthub.modules.features.user.exception;

import com.connecthub.common.exception.DuplicateException;
import com.connecthub.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicateUsernameException extends DuplicateException {


    public DuplicateUsernameException(String keyAttribute, String attributeValue) {
        super(ErrorCode.DUPLICATE_USERNAME, keyAttribute, attributeValue);
    }

}
