package com.markflux.common.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {

    public BusinessException(String errorCode, String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, errorCode, message);
    }

    public BusinessException(String errorCode, String message, Map<String, Object> details) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, errorCode, message, details);
    }
}
