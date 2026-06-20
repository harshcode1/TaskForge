package com.projectmgmttool.backend.exception;

import java.time.LocalDateTime;

public class EmailAlreadyExistsException extends RuntimeException {
    private final LocalDateTime timestamp;

    public EmailAlreadyExistsException(String message) {
        super(message);
        this.timestamp = LocalDateTime.now();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
