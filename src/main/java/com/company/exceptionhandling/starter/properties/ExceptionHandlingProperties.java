package com.company.exceptionhandling.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for exception handling.
 * Modern approach using Java 21 records.
 */
@ConfigurationProperties(prefix = "app.exception")
public record ExceptionHandlingProperties(

    // Whether to include stack traces in responses (dev only)
    boolean includeStackTrace,

    // Whether to include exception causes
    boolean includeCause,

    // Log level for exceptions (ERROR, WARN, INFO)
    String logLevel,

    // Fields to mask in logs (sensitive data)
    List<String> sensitiveFields,

    // Whether to expose internal error codes
    boolean exposeErrorCodes,

    // Base URI for error types (RFC-7807)
    String baseErrorUri

) {

    // Default constructor for Spring Boot
    public ExceptionHandlingProperties() {
        this(false, false, "ERROR", List.of(), true, "https://api.company.com/errors");
    }

    // Constructor with defaults
    public ExceptionHandlingProperties(
            boolean includeStackTrace,
            boolean includeCause,
            String logLevel,
            List<String> sensitiveFields,
            boolean exposeErrorCodes,
            String baseErrorUri) {

        this.includeStackTrace = includeStackTrace;
        this.includeCause = includeCause;
        this.logLevel = logLevel != null ? logLevel : "ERROR";
        this.sensitiveFields = sensitiveFields != null ? List.copyOf(sensitiveFields) : List.of();
        this.exposeErrorCodes = exposeErrorCodes;
        this.baseErrorUri = baseErrorUri != null ? baseErrorUri : "https://api.company.com/errors";
    }

    // Convenience methods
    public boolean shouldLogErrors() {
        return !"OFF".equalsIgnoreCase(logLevel);
    }

    public boolean isSensitiveField(String fieldName) {
        return sensitiveFields.contains(fieldName);
    }

    public String buildErrorTypeUri(String errorCode) {
        return baseErrorUri + "/" + errorCode.toLowerCase();
    }
}
