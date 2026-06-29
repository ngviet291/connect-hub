package com.connecthub.common.exception;

public class InvalidUUIDException extends AppException{


    public InvalidUUIDException() {
        super(ErrorCode.INVALID_UUID);
    }
}
