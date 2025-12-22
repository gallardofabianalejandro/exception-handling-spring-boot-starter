package com.company.exceptionhandling.starter.handler;

import com.company.exceptionhandling.starter.domain.BaseDomainException;
import com.company.exceptionhandling.starter.domain.ValidationException;
import com.company.exceptionhandling.starter.properties.ExceptionHandlingProperties;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Global exception handler for modern Spring Boot applications.
 * Provides automatic exception handling with ProblemDetail responses and observability.
 *
 * This handler is automatically registered by ExceptionHandlingAutoConfiguration
 * and can be overridden by applications using @ConditionalOnMissingBean.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler {

    public static final String TIMESTAMP = "timestamp";
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    public static final String ERROR_HTTP_STATUS = "error.http_status";
    public static final String REQUEST_URI = "requestUri";
    public static final String TRACE_ID = "traceId";
    public static final String ERROR_CODE = "error.code";
    public static final String ERROR_CATEGORY = "error.category";

    private final ExceptionHandlingProperties properties;

    @Autowired(required = false)
    private Tracer tracer;

    public GlobalExceptionHandler(ExceptionHandlingProperties properties) {
        this.properties = properties;
        this.tracer = null; // Default to null, will be injected if available
    }

    @ExceptionHandler(BaseDomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(BaseDomainException ex, WebRequest request) {
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

            // ✅ CRÍTICO: RFC-9457 Type URI
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

            return ResponseEntity.status(ex.getHttpStatus()).body(problem);

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
    public ResponseEntity<ProblemDetail> handleValidationException(ValidationException ex, WebRequest request) {
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

            // ✅ RFC-9457 Type URI
            URI typeUri = URI.create(properties.buildErrorTypeUri(ex.getErrorCode()));
            problem.setType(typeUri);

            problem.setTitle("Validation Failed");
            problem.setProperty(TIMESTAMP, OffsetDateTime.now().toString());
            problem.setProperty(TRACE_ID, traceId);
            problem.setProperty("errorCode", ex.getErrorCode());
            problem.setProperty("fieldErrors", ex.getFieldErrors());
            problem.setProperty("globalErrors", ex.getGlobalErrors());

            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);

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
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, WebRequest request) {
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

            // ✅ RFC-9457 Type URI for generic errors
            URI typeUri = URI.create(properties.buildErrorTypeUri("INTERNAL_SERVER_ERROR"));
            problem.setType(typeUri);

            problem.setProperty(TIMESTAMP, OffsetDateTime.now().toString());
            problem.setProperty(TRACE_ID, traceId);
            problem.setProperty("exceptionType", ex.getClass().getSimpleName());

            if (properties.includeStackTrace()) {
                problem.setProperty("stackTrace", ex.getStackTrace());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> originalMdc = MDC.getCopyOfContextMap();

        try {
            BindingResult result = ex.getBindingResult();

            // 1️⃣ Extraer errores SIN valores sensibles para logs
            Map<String, String> fieldErrors = new HashMap<>();
            Map<String, String> sanitizedFieldErrors = new HashMap<>(); // 👈 Para logs seguros

            for (FieldError error : result.getFieldErrors()) {
                String fieldName = error.getField();
                String errorMessage = error.getDefaultMessage();

                // Para respuesta al cliente (incluye mensaje completo)
                fieldErrors.put(fieldName, errorMessage);

                // Para logs (SIN valores, solo metadata)
                if (properties.isSensitiveField(fieldName)) {
                    sanitizedFieldErrors.put(fieldName, "[REDACTED] - " + errorMessage);
                } else {
                    sanitizedFieldErrors.put(fieldName, errorMessage);
                }
            }

            List<String> globalErrors = result.getGlobalErrors()
                    .stream()
                    .map(ObjectError::getDefaultMessage)
                    .toList();

            // 2️⃣ Setup MDC
            MDC.put(ERROR_CODE, "VALIDATION_ERROR");
            MDC.put(ERROR_HTTP_STATUS, "422");
            MDC.put(ERROR_CATEGORY, "SPRING_VALIDATION");
            MDC.put("validation.fieldCount", String.valueOf(fieldErrors.size()));
            MDC.put("validation.globalCount", String.valueOf(globalErrors.size()));
            MDC.put("validation.objectName", result.getObjectName());

            String traceId = getTraceId();
            String spanId = getSpanId();

            // 3️⃣ Logging estructurado SEGURO (sin PII)
            logger.warn("Spring validation failed",
                    kv(ERROR_CODE, "VALIDATION_ERROR"),
                    kv(TRACE_ID, traceId),
                    kv("spanId", spanId),
                    kv("objectName", result.getObjectName()),
                    kv("fieldErrorCount", fieldErrors.size()),
                    kv("fieldNames", sanitizedFieldErrors.keySet()), // 👈 Solo nombres de campos
                    kv("sanitizedErrors", sanitizedFieldErrors), // 👈 Sin valores sensibles
                    kv("globalErrorCount", globalErrors.size()),
                    kv(REQUEST_URI, request.getDescription(false))
                    // ❌ NO loguear: ex (stack trace con valores)
                    // ❌ NO loguear: fieldErrors directamente (contiene valores)
            );

            // 4️⃣ Construir respuesta RFC-9457
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Validation failed for object '" + result.getObjectName() + "'"
            );

            URI typeUri = URI.create(properties.buildErrorTypeUri("VALIDATION_ERROR"));
            problem.setType(typeUri);

            problem.setTitle("Validation Failed");
            problem.setProperty(TIMESTAMP, OffsetDateTime.now().toString());
            problem.setProperty(TRACE_ID, traceId);
            problem.setProperty("spanId", spanId);
            problem.setProperty("errorCode", "VALIDATION_ERROR");
            problem.setProperty("validationType", "SPRING_FRAMEWORK"); // 👈 Identificador de origen

            // 5️⃣ Respuesta al cliente (puede incluir detalles)
            problem.setProperty("fieldErrors", fieldErrors); // OK para cliente
            problem.setProperty("globalErrors", globalErrors);
            problem.setProperty("errorCount", fieldErrors.size() + globalErrors.size());

            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(problem);

        } finally {
            if (originalMdc != null) {
                MDC.setContextMap(originalMdc);
            } else {
                MDC.remove(ERROR_CODE);
                MDC.remove(ERROR_HTTP_STATUS);
                MDC.remove(ERROR_CATEGORY);
                MDC.remove("validation.fieldCount");
                MDC.remove("validation.globalCount");
                MDC.remove("validation.objectName");
            }
        }
    }
}
