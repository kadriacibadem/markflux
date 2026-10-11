package com.markflux.common.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }

    public ConflictException(String errorCode, String message, Map<String, Object> details) {
        super(HttpStatus.CONFLICT, errorCode, message, details);
    }
}
