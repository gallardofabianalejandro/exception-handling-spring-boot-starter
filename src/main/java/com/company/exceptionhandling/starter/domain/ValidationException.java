package com.company.exceptionhandling.starter.domain;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exception for validation errors.
 */
public final class ValidationException extends BaseDomainException {

    private final Map<String, String> fieldErrors;
    private final List<String> globalErrors;

    private ValidationException(String message, String errorCode,
                              HttpStatus httpStatus, Map<String, Object> details,
                              Map<String, String> fieldErrors, List<String> globalErrors) {
        super(message, errorCode, httpStatus, details);
        this.fieldErrors = fieldErrors != null ? Map.copyOf(fieldErrors) : Map.of();
        this.globalErrors = globalErrors != null ? List.copyOf(globalErrors) : List.of();
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public List<String> getGlobalErrors() {
        return globalErrors;
    }

    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }

    public boolean hasGlobalErrors() {
        return !globalErrors.isEmpty();
    }

    public static Builder builder(String errorCode, String message) {
        return new Builder(errorCode, message);
    }

    public static final class Builder extends BaseDomainException.Builder {

        private final Map<String, String> fieldErrors = new HashMap<>();
        private final List<String> globalErrors = new ArrayList<>();

        private Builder(String errorCode, String message) {
            super(errorCode, message);
            super.httpStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        public Builder fieldError(String field, String message) {
            this.fieldErrors.put(field, message);
            return this;
        }

        public Builder fieldErrors(Map<String, String> errors) {
            this.fieldErrors.putAll(errors);
            return this;
        }

        public Builder globalError(String message) {
            this.globalErrors.add(message);
            return this;
        }

        public Builder globalErrors(List<String> errors) {
            this.globalErrors.addAll(errors);
            return this;
        }

        // ✅ CORRECTED: Uses protected getters from parent
        public ValidationException build() {
            String finalMessage = getMessage();
            if (finalMessage == null || finalMessage.isBlank()) {
                finalMessage = "Validation failed with " + fieldErrors.size() + " field errors";
            }

            Map<String, Object> allDetails = new HashMap<>(getDetails());
            allDetails.put("fieldErrors", Map.copyOf(fieldErrors));
            allDetails.put("globalErrors", List.copyOf(globalErrors));
            allDetails.put("errorCount", fieldErrors.size() + globalErrors.size());
            allDetails.put("category", "VALIDATION");

            return new ValidationException(
                finalMessage,
                getErrorCode(),
                getHttpStatus(),
                allDetails,
                Map.copyOf(fieldErrors),
                List.copyOf(globalErrors)
            );
        }
    }

    // Factory method for Spring BindingResult
    public static ValidationException fromBindingResult(BindingResult result) {
        Builder builder = ValidationException.builder("VALIDATION_ERROR",
                "Validation failed for object '" + result.getObjectName() + "'");

        for (FieldError error : result.getFieldErrors()) {
            builder.fieldError(error.getField(), error.getDefaultMessage());
        }

        for (var error : result.getGlobalErrors()) {
            builder.globalError(error.getDefaultMessage());
        }

        return builder.build();
    }

    public static ValidationException fieldError(String field, String message) {
        return ValidationException.builder("FIELD_VALIDATION_ERROR",
                "Field validation failed")
            .fieldError(field, message)
            .build();
    }

    public static ValidationException fieldErrors(Map<String, String> errors) {
        return ValidationException.builder("MULTIPLE_FIELD_VALIDATION_ERROR",
                "Multiple field validation errors")
            .fieldErrors(errors)
            .build();
    }
}
