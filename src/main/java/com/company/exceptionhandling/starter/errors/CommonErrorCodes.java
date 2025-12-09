package com.company.exceptionhandling.starter.errors;

import com.company.exceptionhandling.starter.domain.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Common error codes for general use.
 * Provides type safety and prevents typos.
 */
public interface CommonErrorCodes {

    // Generic business errors
    ErrorCode RESOURCE_NOT_FOUND = ErrorCode.of(
        "RESOURCE_NOT_FOUND",
        "Resource not found",
        HttpStatus.NOT_FOUND
    );

    ErrorCode RESOURCE_ALREADY_EXISTS = ErrorCode.of(
        "RESOURCE_ALREADY_EXISTS",
        "Resource already exists",
        HttpStatus.CONFLICT
    );

    // Validation errors
    ErrorCode VALIDATION_ERROR = ErrorCode.of(
        "VALIDATION_ERROR",
        "Validation failed",
        HttpStatus.UNPROCESSABLE_ENTITY
    );

    ErrorCode FIELD_VALIDATION_ERROR = ErrorCode.of(
        "FIELD_VALIDATION_ERROR",
        "Field validation failed",
        HttpStatus.UNPROCESSABLE_ENTITY
    );

    ErrorCode MULTIPLE_FIELD_VALIDATION_ERROR = ErrorCode.of(
        "MULTIPLE_FIELD_VALIDATION_ERROR",
        "Multiple field validation errors",
        HttpStatus.UNPROCESSABLE_ENTITY
    );

    // Authorization errors
    ErrorCode UNAUTHORIZED = ErrorCode.of(
        "UNAUTHORIZED",
        "Unauthorized access",
        HttpStatus.UNAUTHORIZED
    );

    ErrorCode FORBIDDEN = ErrorCode.of(
        "FORBIDDEN",
        "Access forbidden",
        HttpStatus.FORBIDDEN
    );

    // Generic server errors
    ErrorCode INTERNAL_SERVER_ERROR = ErrorCode.of(
        "INTERNAL_SERVER_ERROR",
        "Internal server error",
        HttpStatus.INTERNAL_SERVER_ERROR
    );

    ErrorCode SERVICE_UNAVAILABLE = ErrorCode.of(
        "SERVICE_UNAVAILABLE",
        "Service temporarily unavailable",
        HttpStatus.SERVICE_UNAVAILABLE
    );
}
