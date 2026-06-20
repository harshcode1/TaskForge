package com.projectmgmttool.backend.exception;

import java.time.LocalDateTime;

public class UserNotFoundException extends RuntimeException {
    private final LocalDateTime timestamp;

    public UserNotFoundException(String message) {
        super(message);
        this.timestamp = LocalDateTime.now();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
