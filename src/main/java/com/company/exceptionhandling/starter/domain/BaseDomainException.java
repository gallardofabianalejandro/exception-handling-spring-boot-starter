package com.company.exceptionhandling.starter.domain;

import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all domain exceptions.
 * Uses corrected builder pattern with protected getters.
 */
public abstract class BaseDomainException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Map<String, Object> details;

    protected BaseDomainException(String message, String errorCode,
                                HttpStatus httpStatus, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details != null ? Map.copyOf(details) : Map.of();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public URI getErrorTypeUri(String baseUri) {
        return URI.create(baseUri + "/" + errorCode.toLowerCase());
    }

    // ✅ CORRECTED: Builder with protected getters for subclasses
    public static Builder builder(String errorCode, String message) {
        return new Builder(errorCode, message);
    }

    public static Builder builder(ErrorCode errorCode) {
        return new Builder(errorCode.code(), errorCode.defaultMessage())
            .httpStatus(errorCode.httpStatus());
    }

    public static class Builder {
        private final String errorCode;
        private final String message;
        private HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        private final Map<String, Object> details = new HashMap<>();

        // Package-private constructor
        Builder(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        public Builder httpStatus(HttpStatus status) {
            this.httpStatus = status;
            return this;
        }

        public Builder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details.putAll(details);
            return this;
        }

        // ✅ CORRECTED: Protected getters for subclasses
        protected String getErrorCode() { return errorCode; }
        protected String getMessage() { return message; }
        protected HttpStatus getHttpStatus() { return httpStatus; }
        protected Map<String, Object> getDetails() { return Map.copyOf(details); }

        // ✅ Factory method for subclasses to create the exception
        protected final BaseDomainException doBuild() {
            return new BaseDomainException(message, errorCode, httpStatus, Map.copyOf(details)) {};
        }

        // Convenience methods
        public Builder badRequest() { return httpStatus(HttpStatus.BAD_REQUEST); }
        public Builder notFound() { return httpStatus(HttpStatus.NOT_FOUND); }
        public Builder conflict() { return httpStatus(HttpStatus.CONFLICT); }
        public Builder unprocessableEntity() { return httpStatus(HttpStatus.UNPROCESSABLE_ENTITY); }
    }

    // ✅ ADDED: Convenience methods
    public boolean isClientError() {
        return httpStatus.is4xxClientError();
    }

    public boolean isServerError() {
        return httpStatus.is5xxServerError();
    }

    public boolean isBadRequest() {
        return httpStatus == HttpStatus.BAD_REQUEST;
    }

    public boolean isNotFound() {
        return httpStatus == HttpStatus.NOT_FOUND;
    }
}
