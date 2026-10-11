package com.markflux.common.error;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends ApiException {

    public ServiceUnavailableException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, errorCode, message);
        initCause(cause);
    }
}
