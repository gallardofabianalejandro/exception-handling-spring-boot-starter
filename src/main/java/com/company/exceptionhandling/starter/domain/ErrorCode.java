package com.company.exceptionhandling.starter.domain;

import org.springframework.http.HttpStatus;

/**
 * Type-safe error codes with metadata.
 * Provides compile-time safety and prevents typos.
 */
public record ErrorCode(
    String code,
    String defaultMessage,
    HttpStatus httpStatus
) {

    /**
     * Creates an ErrorCode with all parameters.
     */
    public static ErrorCode of(String code, String defaultMessage, HttpStatus httpStatus) {
        return new ErrorCode(code, defaultMessage, httpStatus);
    }

    /**
     * Creates an ErrorCode with default message derived from code.
     * Converts SNAKE_CASE to "snake case".
     */
    public static ErrorCode of(String code, HttpStatus httpStatus) {
        String defaultMessage = code.replace("_", " ").toLowerCase();
        return new ErrorCode(code, defaultMessage, httpStatus);
    }
}
