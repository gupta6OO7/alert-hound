package com.incident.alerthound.logprocessor.service;

public class NonRetryableProcessingException extends RuntimeException {

    public NonRetryableProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
