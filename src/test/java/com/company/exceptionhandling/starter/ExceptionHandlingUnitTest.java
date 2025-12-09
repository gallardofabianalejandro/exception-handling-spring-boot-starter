package com.company.exceptionhandling.starter;

import com.company.exceptionhandling.starter.domain.ValidationException;
import com.company.exceptionhandling.starter.properties.ExceptionHandlingProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for exception handling components.
 */
class ExceptionHandlingUnitTest {

    @Test
    void validationExceptionCreation() {
        // Test validation exception creation
        ValidationException exception = ValidationException.builder("VALIDATION_ERROR", "Validation failed")
            .fieldError("email", "Invalid format")
            .fieldError("age", "Must be positive")
            .globalError("Object validation failed")
            .build();

        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertEquals(422, exception.getHttpStatus().value());
        assertTrue(exception.hasFieldErrors());
        assertTrue(exception.hasGlobalErrors());
        assertEquals(2, exception.getFieldErrors().size());
        assertEquals(1, exception.getGlobalErrors().size());
    }

    @Test
    void exceptionHandlingPropertiesDefaults() {
        // Test default properties
        ExceptionHandlingProperties props = new ExceptionHandlingProperties(
            false, false, null, null, true, null
        );

        assertFalse(props.includeStackTrace());
        assertFalse(props.includeCause());
        assertEquals("ERROR", props.logLevel());
        assertTrue(props.sensitiveFields().isEmpty());
        assertTrue(props.exposeErrorCodes());
        assertEquals("https://api.company.com/errors", props.baseErrorUri());
        assertEquals("https://api.company.com/errors/test-error", props.buildErrorTypeUri("test-error"));
    }

    @Test
    void exceptionHandlingPropertiesCustom() {
        // Test custom properties
        ExceptionHandlingProperties props = new ExceptionHandlingProperties(
            true, true, "WARN", java.util.List.of("password", "ssn"), false, "https://myapi.com/errors"
        );

        assertTrue(props.includeStackTrace());
        assertTrue(props.includeCause());
        assertEquals("WARN", props.logLevel());
        assertEquals(2, props.sensitiveFields().size());
        assertTrue(props.sensitiveFields().contains("password"));
        assertTrue(props.sensitiveFields().contains("ssn"));
        assertFalse(props.exposeErrorCodes());
        assertEquals("https://myapi.com/errors", props.baseErrorUri());
        assertEquals("https://myapi.com/errors/custom", props.buildErrorTypeUri("custom"));
    }
}
