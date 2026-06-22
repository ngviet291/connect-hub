package com.connecthub.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {

    private final BaseErrorCode errorCode;


    public AppException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(String string){
        super(string);
        this.errorCode = ErrorCode.BAD_REQUEST;
    }

}