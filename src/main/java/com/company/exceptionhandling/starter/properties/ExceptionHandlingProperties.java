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

        // Base URI for error types (RFC-9457)
        String baseErrorUri

) {

    // Default constructor for Spring Boot
    public ExceptionHandlingProperties() {
        this(
                false,
                false,
                "ERROR",
                List.of("password", "email", "ssn", "creditCard", "phoneNumber"), // 👈 Defaults seguros
                true,
                "https://api.company.com/errors"
        );
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

    /**
     * Determines if a field contains sensitive information and should be redacted in logs.
     * Supports both simple field names and nested paths.
     *
     * Examples:
     * - "password" matches "password"
     * - "email" matches "email", "user.email", "customer.email"
     * - "balance" matches "account.balance"
     *
     * @param fieldName the field name to check (can be nested path like "user.email")
     * @return true if the field should be considered sensitive and redacted
     */
    public boolean isSensitiveField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }

        // Check for exact match or nested field match
        return sensitiveFields.stream()
                .anyMatch(sensitiveField ->
                        // Exact match: "email" == "email"
                        fieldName.equals(sensitiveField) ||
                                // Nested field: "user.email" ends with ".email"
                                fieldName.endsWith("." + sensitiveField) ||
                                // Middle nested: "data.user.email.verified" contains ".email."
                                fieldName.contains("." + sensitiveField + ".")
                );
    }

    /**
     * Builds a RFC-9457 compliant error type URI for a given error code.
     * Converts error codes to lowercase and replaces underscores with hyphens.
     *
     * Example:
     * - "VALIDATION_ERROR" → "https://api.company.com/errors/validation-error"
     * - "CUSTOMER_NOT_FOUND" → "https://api.company.com/errors/customer-not-found"
     *
     * @param errorCode the error code
     * @return full URI for the error type
     */
    public String buildErrorTypeUri(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return baseErrorUri + "/unknown";
        }
        return baseErrorUri + "/" + errorCode.toLowerCase().replace("_", "-");
    }
}