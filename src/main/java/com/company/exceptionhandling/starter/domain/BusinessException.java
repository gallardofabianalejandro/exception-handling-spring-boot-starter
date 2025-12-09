package com.company.exceptionhandling.starter.domain;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Exception for business logic violations.
 */
public final class BusinessException extends BaseDomainException {

    public static final String CATEGORY = "category";
    public static final String BUSINESS_RULE = "BUSINESS_RULE";

    private BusinessException(String message, String errorCode,
                              HttpStatus httpStatus, Map<String, Object> details) {
        super(message, errorCode, httpStatus, details);
    }

    public static Builder builder(String errorCode, String message) {
        return new Builder(errorCode, message);
    }

    public static Builder builder(ErrorCode errorCode) {
        return new Builder(errorCode.code(), errorCode.defaultMessage())
            .httpStatus(errorCode.httpStatus());
    }

    public static final class Builder extends BaseDomainException.Builder {

        private Builder(String errorCode, String message) {
            super(errorCode, message);
        }

        // ✅ CORRECTED: Uses doBuild() from parent
        public BusinessException build() {
            return new BusinessException(
                getMessage(),
                getErrorCode(),
                getHttpStatus(),
                getDetails()
            );
        }

        // ✅ CORRECTED: Proper override
        @Override
        public Builder httpStatus(HttpStatus status) {
            super.httpStatus(status);
            return this;
        }

        @Override
        public Builder detail(String key, Object value) {
            super.detail(key, value);
            return this;
        }
    }

    // Factory methods using ErrorCode (type-safe)
    public static BusinessException of(ErrorCode errorCode, Object... details) {
        Builder builder = BusinessException.builder(errorCode);
        for (int i = 0; i < details.length; i += 2) {
            if (i + 1 < details.length) {
                builder.detail(details[i].toString(), details[i + 1]);
            }
        }
        return builder.build();
    }

    // Legacy factory methods (still supported)
    public static BusinessException customerAlreadyExists(String customerId) {
        return new BusinessException(
            "Customer with ID '" + customerId + "' already exists",
            "CUSTOMER_ALREADY_EXISTS",
            HttpStatus.CONFLICT,
            Map.of("customerId", customerId, CATEGORY, BUSINESS_RULE)
        );
    }

    public static BusinessException customerNotFound(String customerId) {
        return new BusinessException(
            "Customer with ID '" + customerId + "' not found",
            "CUSTOMER_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            Map.of("customerId", customerId, CATEGORY, BUSINESS_RULE)
        );
    }

    public static BusinessException insufficientFunds(String accountId, double required, double available) {
        return new BusinessException(
            "Insufficient funds for transaction",
            "INSUFFICIENT_FUNDS",
            HttpStatus.BAD_REQUEST,
            Map.of(
                "accountId", accountId,
                "required", required,
                "available", available,
                "shortfall", required - available,
                    CATEGORY, BUSINESS_RULE
            )
        );
    }
}
