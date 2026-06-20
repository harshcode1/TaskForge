package com.projectmgmttool.backend.exception;

import java.util.Map;

public class ErrorResponse {
    private int status;
    private String message;
    private long timestamp;
    private String error;
    private String path;
    private Map<String, String> validationErrors;

    private ErrorResponse(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.timestamp = builder.timestamp;
        this.error = builder.error;
        this.path = builder.path;
        this.validationErrors = builder.validationErrors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int status;
        private String message;
        private long timestamp;
        private String error;
        private String path;
        private Map<String, String> validationErrors;

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder validationErrors(Map<String, String> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getError() {
        return error;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

}
