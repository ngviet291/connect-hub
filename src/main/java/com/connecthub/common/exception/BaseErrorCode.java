package com.connecthub.common.exception;

import org.springframework.http.HttpStatusCode;

public interface BaseErrorCode {
    String getMessage();
    HttpStatusCode getStatusCode();
}