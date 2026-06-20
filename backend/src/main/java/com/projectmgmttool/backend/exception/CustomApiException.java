package com.projectmgmttool.backend.exception;

import java.time.LocalDateTime;

public class CustomApiException extends RuntimeException {
    private final int errorCode;
    private final LocalDateTime timestamp;

    public CustomApiException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }

    public int getErrorCode() {
        return errorCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
