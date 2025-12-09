package com.company.exceptionhandling.starter.handler;

import com.company.exceptionhandling.starter.domain.BaseDomainException;
import com.company.exceptionhandling.starter.domain.ValidationException;
import com.company.exceptionhandling.starter.properties.ExceptionHandlingProperties;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Modern global exception handler with observability and ProblemDetail.
 */
@ControllerAdvice
public class StarterGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String TIMESTAMP = "timestamp";
    private static final Logger logger = LoggerFactory.getLogger(StarterGlobalExceptionHandler.class);
    public static final String ERROR_HTTP_STATUS = "error.http_status";
    public static final String REQUEST_URI = "requestUri";
    public static final String TRACE_ID = "traceId";
    public static final String ERROR_CODE = "error.code";
    public static final String ERROR_CATEGORY = "error.category";

    private final ExceptionHandlingProperties properties;
    private final Tracer tracer;

    public StarterGlobalExceptionHandler(ExceptionHandlingProperties properties, Tracer tracer) {
        this.properties = properties;
        this.tracer = tracer;
    }

    @ExceptionHandler(BaseDomainException.class)
    public ProblemDetail handleDomainException(BaseDomainException ex, WebRequest request) {
        // ✅ CORRECTED: Save original MDC to prevent memory leaks
        Map<String, String> originalMdc = MDC.getCopyOfContextMap();

        try {
            // ✅ CORRECTED: Add only our specific fields
            MDC.put(ERROR_CODE, ex.getErrorCode());
            MDC.put(ERROR_HTTP_STATUS, String.valueOf(ex.getHttpStatus().value()));
            MDC.put(ERROR_CATEGORY, getErrorCategory(ex));

            String traceId = getTraceId();
            String spanId = getSpanId();

            // Structured Logging (enterprise-ready)
            logger.error("Domain exception occurred",
                kv(ERROR_CODE, ex.getErrorCode()),
                kv("httpStatus", ex.getHttpStatus().value()),
                kv(TRACE_ID, traceId),
                kv("spanId", spanId),
                kv("details", ex.getDetails()),
                kv(REQUEST_URI, request.getDescription(false)),
                ex
            );

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                ex.getHttpStatus(),
                ex.getMessage()
            );

            // ✅ CRÍTICO: RFC-7807 Type URI
            URI typeUri = URI.create(properties.buildErrorTypeUri(ex.getErrorCode()));
            problem.setType(typeUri);

            problem.setProperty(TIMESTAMP, OffsetDateTime.now().toString());
            problem.setProperty(TRACE_ID, traceId);
            problem.setProperty("spanId", spanId);
            problem.setProperty(ERROR_CODE, ex.getErrorCode());

            ex.getDetails().forEach(problem::setProperty);

            if (properties.includeStackTrace()) {
                problem.setProperty("stackTrace", ex.getStackTrace());
            }

            return problem;

        } finally {
            // ✅ CORRECTED: Restore original MDC (no MDC.clear())
            if (originalMdc != null) {
                MDC.setContextMap(originalMdc);
            } else {
                // Clean only our specific fields if no original MDC existed
                MDC.remove(ERROR_CODE);
                MDC.remove(ERROR_HTTP_STATUS);
                MDC.remove(ERROR_CATEGORY);
            }
        }
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException ex, WebRequest request) {
        Map<String, String> originalMdc = MDC.getCopyOfContextMap();

        try {
            MDC.put(ERROR_CODE, ex.getErrorCode());
            MDC.put(ERROR_HTTP_STATUS, "422");
            MDC.put(ERROR_CATEGORY, "VALIDATION");
            MDC.put("validation.fieldCount", String.valueOf(ex.getFieldErrors().size()));
            MDC.put("validation.globalCount", String.valueOf(ex.getGlobalErrors().size()));

            String traceId = getTraceId();

            logger.warn("Validation exception occurred",
                kv("errorCode", ex.getErrorCode()),
                kv(TRACE_ID, traceId),
                kv("fieldErrors", ex.getFieldErrors()),
                kv("globalErrors", ex.getGlobalErrors()),
                kv(REQUEST_URI, request.getDescription(false)),
                ex
            );

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
            );

            // ✅ RFC-7807 Type URI
            URI typeUri = URI.create(properties.buildErrorTypeUri(ex.getErrorCode()));
            problem.setType(typeUri);

            problem.setTitle("Validation Failed");
            problem.setProperty(TIMESTAMP, OffsetDateTime.now().toString());
            problem.setProperty(TRACE_ID, traceId);
            problem.setProperty("errorCode", ex.getErrorCode());
            problem.setProperty("fieldErrors", ex.getFieldErrors());
            problem.setProperty("globalErrors", ex.getGlobalErrors());

            return problem;

        } finally {
            if (originalMdc != null) {
                MDC.setContextMap(originalMdc);
            } else {
                MDC.remove(ERROR_CODE);
                MDC.remove(ERROR_HTTP_STATUS);
                MDC.remove(ERROR_CATEGORY);
                MDC.remove("validation.fieldCount");
                MDC.remove("validation.globalCount");
            }
        }
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        Map<String, String> originalMdc = MDC.getCopyOfContextMap();

        try {
            MDC.put(ERROR_HTTP_STATUS, "500");
            MDC.put(ERROR_CATEGORY, "GENERIC");

            String traceId = getTraceId();

            logger.error("Unexpected error occurred: %s",
                ex.getClass().getSimpleName(),
                kv("exceptionType", ex.getClass().getSimpleName()),
                kv(TRACE_ID, traceId),
                kv(REQUEST_URI, request.getDescription(false)),
                ex
            );

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
            );

            // ✅ RFC-7807 Type URI for generic errors
            URI typeUri = URI.create(properties.buildErrorTypeUri("INTERNAL_SERVER_ERROR"));
            problem.setType(typeUri);

            problem.setProperty(TIMESTAMP, OffsetDateTime.now().toString());
            problem.setProperty(TRACE_ID, traceId);
            problem.setProperty("exceptionType", ex.getClass().getSimpleName());

            if (properties.includeStackTrace()) {
                problem.setProperty("stackTrace", ex.getStackTrace());
            }

            return problem;

        } finally {
            if (originalMdc != null) {
                MDC.setContextMap(originalMdc);
            } else {
                MDC.remove(ERROR_HTTP_STATUS);
                MDC.remove(ERROR_CATEGORY);
            }
        }
    }

    private String getTraceId() {
        return tracer != null && tracer.currentSpan() != null ?
            tracer.currentSpan().context().traceId() : "unknown";
    }

    private String getSpanId() {
        return tracer != null && tracer.currentSpan() != null ?
            tracer.currentSpan().context().spanId() : "unknown";
    }

    private String getErrorCategory(BaseDomainException ex) {
        if (ex instanceof ValidationException) {
            return "VALIDATION";
        } else {
            return "BUSINESS";
        }
    }
}
