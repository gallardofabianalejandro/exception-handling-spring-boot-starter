# Exception Handling Spring Boot Starter

Un starter moderno de Spring Boot para el manejo completo de excepciones con soporte para ProblemDetail (RFC 9457), logging estructurado y características de observabilidad.

## Características

- **Soporte para ProblemDetail**: Respuestas de error modernas siguiendo RFC 9457
- **Logging Estructurado**: Logging listo para empresas con contexto MDC y IDs de trace/span
- **Excepciones de Dominio**: Jerarquía de excepciones type-safe con patrón builder
- **Soporte para Validación**: Integración con Spring Validation y excepciones de validación personalizadas
- **Observabilidad**: Integración con Micrometer Tracing para tracing distribuido
- **Configurable**: Opciones extensas de configuración vía propiedades de aplicación
- **Java 21**: Construido con características modernas de Java y records

## Inicio Rápido

### 1. Agregar Dependencia

```xml
<dependency>
    <groupId>com.company</groupId>
    <artifactId>exception-handling-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Auto-Configuración

El starter se configura automáticamente cuando se agrega al classpath. ¡No se requiere configuración adicional!



## Arquitectura y Componentes

### Clase ExceptionHandlingAutoConfiguration

Configuración automática principal del starter. Registra automáticamente el `GlobalExceptionHandler` usando `@ConditionalOnMissingBean`, permitiendo que las aplicaciones lo sobrescriban si es necesario.

```java
@AutoConfiguration
@EnableConfigurationProperties(ExceptionHandlingProperties.class)
public class ExceptionHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler(
            ExceptionHandlingProperties properties,
            Tracer tracer) {
        return new GlobalExceptionHandler(properties, tracer);
    }
}
```

**Funcionamiento**: Se registra automáticamente a través del archivo `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### Clase ExceptionHandlingStarterConfiguration

Configuración básica del starter. Actualmente vacía, pero proporciona un hook para configuraciones manuales futuras.

### Propiedades de Configuración (ExceptionHandlingProperties)

Record moderno de Java 21 que maneja todas las propiedades de configuración del starter.

```java
@ConfigurationProperties(prefix = "app.exception")
public record ExceptionHandlingProperties(
    boolean includeStackTrace,
    boolean includeCause,
    String logLevel,
    List<String> sensitiveFields,
    boolean exposeErrorCodes,
    String baseErrorUri
) {
    // Constructor con valores por defecto
    public ExceptionHandlingProperties() {
        this(false, false, "ERROR", List.of(), true, "https://api.company.com/errors");
    }

    // Métodos de conveniencia
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
```

**Propiedades disponibles**:

| Propiedad | Valor por Defecto | Descripción |
|-----------|-------------------|-------------|
| `app.exception.include-stack-trace` | `false` | Incluir stack traces en respuestas de error |
| `app.exception.include-cause` | `false` | Incluir causas de excepciones |
| `app.exception.log-level` | `ERROR` | Nivel de log para excepciones (ERROR, WARN, INFO, OFF) |
| `app.exception.sensitive-fields` | `[]` | Lista de nombres de campos sensibles a enmascarar en logs |
| `app.exception.expose-error-codes` | `true` | Si exponer códigos de error internos |
| `app.exception.base-error-uri` | `https://api.company.com/errors` | URI base para links de tipos de error |

### Jerarquía de Excepciones de Dominio

#### BaseDomainException

Clase abstracta base para todas las excepciones de dominio. Implementa patrón builder corregido con getters protegidos para subclases.

```java
public abstract class BaseDomainException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Map<String, Object> details;

    protected BaseDomainException(String message, String errorCode,
                                HttpStatus httpStatus, Map<String, Object> details) {
        // Constructor protegido
    }

    // Getters públicos
    public String getErrorCode() { return errorCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public Map<String, Object> getDetails() { return details; }

    // Builder pattern con getters protegidos
    public static Builder builder(String errorCode, String message) {
        return new Builder(errorCode, message);
    }

    public static class Builder {
        // Campos privados
        private final String errorCode;
        private final String message;
        private HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        private final Map<String, Object> details = new HashMap<>();

        Builder(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        // Getters PROTEGIDOS para subclases
        protected String getErrorCode() { return errorCode; }
        protected String getMessage() { return message; }
        protected HttpStatus getHttpStatus() { return httpStatus; }
        protected Map<String, Object> getDetails() { return Map.copyOf(details); }

        // Método protegido para que subclases construyan la excepción
        protected final BaseDomainException doBuild() {
            return new BaseDomainException(message, errorCode, httpStatus, Map.copyOf(details)) {};
        }
    }

    // Métodos de conveniencia
    public boolean isClientError() { return httpStatus.is4xxClientError(); }
    public boolean isServerError() { return httpStatus.is5xxServerError(); }
    public boolean isBadRequest() { return httpStatus == HttpStatus.BAD_REQUEST; }
    public boolean isNotFound() { return httpStatus == HttpStatus.NOT_FOUND; }
}
```

**Funcionamiento**:
- Almacena código de error, status HTTP y detalles adicionales
- Patrón builder permite construcción fluida
- Getters protegidos permiten que subclases accedan a valores durante construcción
- Métodos de conveniencia para categorizar errores

#### BusinessException

Excepción para violaciones de lógica de negocio. Extiende `BaseDomainException` con funcionalidades específicas.

```java
public final class BusinessException extends BaseDomainException {

    public static final String CATEGORY = "category";
    public static final String BUSINESS_RULE = "BUSINESS_RULE";

    private BusinessException(String message, String errorCode,
                              HttpStatus httpStatus, Map<String, Object> details) {
        super(message, errorCode, httpStatus, details);
    }

    // Builder que extiende BaseDomainException.Builder
    public static final class Builder extends BaseDomainException.Builder {

        private Builder(String errorCode, String message) {
            super(errorCode, message);
        }

        public BusinessException build() {
            return new BusinessException(
                getMessage(),
                getErrorCode(),
                getHttpStatus(),
                getDetails()
            );
        }
    }

    // Factory methods usando ErrorCode (type-safe)
    public static BusinessException of(ErrorCode errorCode, Object... details) {
        Builder builder = BusinessException.builder(errorCode);
        for (int i = 0; i < details.length; i += 2) {
            if (i + 1 < details.length) {
                builder.detail(details[i].toString(), details[i + 1]);
            }
        }
        return builder.build();
    }

    // Métodos legacy (todavía soportados)
    public static BusinessException customerAlreadyExists(String customerId) {
        return new BusinessException(
            "Customer with ID '" + customerId + "' already exists",
            "CUSTOMER_ALREADY_EXISTS",
            HttpStatus.CONFLICT,
            Map.of("customerId", customerId, CATEGORY, BUSINESS_RULE)
        );
    }
}
```

**Funcionamiento**:
- Constructor privado para control de instanciación
- Builder que usa getters protegidos de la clase padre
- Métodos factory para creación conveniente
- Categorización automática como "BUSINESS_RULE"

#### ValidationException

Excepción para errores de validación. Maneja errores de campo y globales.

```java
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

    // Getters específicos
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public List<String> getGlobalErrors() { return globalErrors; }
    public boolean hasFieldErrors() { return !fieldErrors.isEmpty(); }
    public boolean hasGlobalErrors() { return !globalErrors.isEmpty(); }

    // Builder específico para validación
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

    // Factory method para Spring BindingResult
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

    // Métodos factory convenientes
    public static ValidationException fieldError(String field, String message) {
        return ValidationException.builder("FIELD_VALIDATION_ERROR",
                "Field validation failed")
            .fieldError(field, message)
            .build();
    }
}
```

**Funcionamiento**:
- Maneja errores de campo (field-specific) y errores globales
- Constructor privado con validación de campos
- Builder que configura automáticamente status 422 (Unprocessable Entity)
- Integración automática con Spring Validation a través de `fromBindingResult()`

### Sistema de Códigos de Error

#### ErrorCode

Record type-safe para códigos de error con metadatos.

```java
public record ErrorCode(
    String code,
    String defaultMessage,
    HttpStatus httpStatus
) {
    // Constructor con conversión automática de SNAKE_CASE
    public static ErrorCode of(String code, HttpStatus httpStatus) {
        String defaultMessage = code.replace("_", " ").toLowerCase();
        return new ErrorCode(code, defaultMessage, httpStatus);
    }

    // Constructor completo
    public static ErrorCode of(String code, String defaultMessage, HttpStatus httpStatus) {
        return new ErrorCode(code, defaultMessage, httpStatus);
    }
}
```

**Funcionamiento**:
- Inmutable por diseño (record)
- Conversión automática de códigos SNAKE_CASE a mensajes legibles
- Type-safe: previene errores de tipeo

#### CommonErrorCodes

Interface con códigos de error comunes predefinidos.

```java
public interface CommonErrorCodes {

    // Errores genéricos de negocio
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

    // Errores de validación
    ErrorCode VALIDATION_ERROR = ErrorCode.of(
        "VALIDATION_ERROR",
        "Validation failed",
        HttpStatus.UNPROCESSABLE_ENTITY
    );

    // Errores de autorización
    ErrorCode UNAUTHORIZED = ErrorCode.of(
        "UNAUTHORIZED",
        "Unauthorized access",
        HttpStatus.UNAUTHORIZED
    );

    // Errores de servidor genéricos
    ErrorCode INTERNAL_SERVER_ERROR = ErrorCode.of(
        "INTERNAL_SERVER_ERROR",
        "Internal server error",
        HttpStatus.INTERNAL_SERVER_ERROR
    );
}
```

**Funcionamiento**:
- Constantes finales que no pueden ser modificadas
- Agrupación lógica de errores comunes
- Reutilizables en toda la aplicación

### GlobalExceptionHandler

Manejador global de excepciones con logging estructurado y respuestas ProblemDetail.

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    private final ExceptionHandlingProperties properties;
    private final Tracer tracer;

    public GlobalExceptionHandler(ExceptionHandlingProperties properties, Tracer tracer) {
        this.properties = properties;
        this.tracer = tracer;
    }

    @ExceptionHandler(BaseDomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(BaseDomainException ex, WebRequest request) {
        // Preserva MDC original para evitar fugas de memoria
        Map<String, String> originalMdc = MDC.getCopyOfContextMap();

        try {
            // Agrega campos específicos al MDC
            MDC.put(ERROR_CODE, ex.getErrorCode());
            MDC.put(ERROR_HTTP_STATUS, String.valueOf(ex.getHttpStatus().value()));
            MDC.put(ERROR_CATEGORY, getErrorCategory(ex));

            // Logging estructurado con trace ID
            String traceId = getTraceId();
            logger.error("Domain exception occurred", /* argumentos estructurados */);

            // Construye respuesta ProblemDetail
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                ex.getHttpStatus(),
                ex.getMessage()
            );

            // URI de tipo RFC-9457
            URI typeUri = URI.create(properties.buildErrorTypeUri(ex.getErrorCode()));
            problem.setType(typeUri);

            // Agrega propiedades adicionales
            problem.setProperty(TIMESTAMP, OffsetDateTime.now().toString());
            problem.setProperty(TRACE_ID, traceId);
            problem.setProperty("errorCode", ex.getErrorCode());

            ex.getDetails().forEach(problem::setProperty);

            return ResponseEntity.status(ex.getHttpStatus()).body(problem);

        } finally {
            // Restaura MDC original (sin MDC.clear() para evitar fugas)
            if (originalMdc != null) {
                MDC.setContextMap(originalMdc);
            } else {
                // Limpia solo campos específicos
                MDC.remove(ERROR_CODE);
                MDC.remove(ERROR_HTTP_STATUS);
                MDC.remove(ERROR_CATEGORY);
            }
        }
    }

    // Métodos similares para ValidationException y Exception genérica
}
```

**Funcionamiento**:
- Maneja `BaseDomainException`, `ValidationException` y excepciones genéricas
- Logging estructurado con MDC context
- Integración con tracing (traceId/spanId)
- Respuestas ProblemDetail RFC-9457
- Manejo seguro de MDC para evitar fugas de memoria
- Categorización automática de errores

## Incorporación del Starter a un Proyecto

### Paso 1: Agregar Dependencia

En tu `pom.xml`:

```xml
<dependency>
    <groupId>com.company</groupId>
    <artifactId>exception-handling-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Paso 2: Configuración (Opcional)

En `application.properties` o `application.yml`:

```properties
# Configuración del manejo de excepciones
app.exception.include-stack-trace=false
app.exception.log-level=ERROR
app.exception.sensitive-fields=user.password,account.balance
app.exception.base-error-uri=https://api.mycompany.com/errors
```

### Paso 3: Usar las Excepciones

El starter se configura automáticamente. Puedes empezar a usar las excepciones inmediatamente:

```java
@Service
public class UserService {

    public User findUser(String id) {
        if (userNotFound) {
            throw BusinessException.of(CommonErrorCodes.RESOURCE_NOT_FOUND)
                .detail("userId", id)
                .detail("resourceType", "User");
        }
    }
}
```

## Creación de Nuevas Excepciones

### Usando la Jerarquía Existente

#### Opción 1: Crear ErrorCode Personalizado

```java
import com.company.exceptionhandling.starter.domain.ErrorCode;
import com.company.exceptionhandling.starter.domain.BusinessException;
import org.springframework.http.HttpStatus;

// Definir códigos de error personalizados
public interface OrderErrorCodes {
    ErrorCode ORDER_NOT_FOUND = ErrorCode.of(
        "ORDER_NOT_FOUND",
        "Order not found",
        HttpStatus.NOT_FOUND
    );

    ErrorCode INSUFFICIENT_STOCK = ErrorCode.of(
        "INSUFFICIENT_STOCK",
        "Insufficient stock for product",
        HttpStatus.BAD_REQUEST
    );

    ErrorCode PAYMENT_FAILED = ErrorCode.of(
        "PAYMENT_FAILED",
        "Payment processing failed",
        HttpStatus.PAYMENT_REQUIRED
    );
}
```

**Uso:**

```java
@Service
public class OrderService {

    public Order getOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw BusinessException.of(OrderErrorCodes.ORDER_NOT_FOUND)
                .detail("orderId", orderId);
        }
        return order;
    }

    public void placeOrder(OrderRequest request) {
        // Verificar stock
        for (OrderItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId());
            if (product.getStock() < item.getQuantity()) {
                throw BusinessException.of(OrderErrorCodes.INSUFFICIENT_STOCK)
                    .detail("productId", item.getProductId())
                    .detail("requestedQuantity", item.getQuantity())
                    .detail("availableStock", product.getStock());
            }
        }

        // Procesar pago
        try {
            paymentService.processPayment(request.getPaymentInfo());
        } catch (PaymentException e) {
            throw BusinessException.of(OrderErrorCodes.PAYMENT_FAILED)
                .detail("amount", request.getTotalAmount())
                .detail("paymentMethod", request.getPaymentInfo().getMethod());
        }
    }
}
```

#### Opción 2: Crear Excepción Personalizada Extendiendo BusinessException

```java
import com.company.exceptionhandling.starter.domain.BaseDomainException;
import com.company.exceptionhandling.starter.domain.ErrorCode;
import org.springframework.http.HttpStatus;
import java.util.Map;

// Excepción personalizada para dominio de órdenes
public class OrderDomainException extends BaseDomainException {

    private static final String DOMAIN = "ORDER";

    protected OrderDomainException(String message, String errorCode,
                                 HttpStatus httpStatus, Map<String, Object> details) {
        super(message, errorCode, httpStatus, details);
    }

    // Builder personalizado
    public static Builder builder(String errorCode, String message) {
        return new Builder(errorCode, message);
    }

    public static Builder builder(ErrorCode errorCode) {
        return new Builder(errorCode.code(), errorCode.defaultMessage())
            .httpStatus(errorCode.httpStatus());
    }

    public static class Builder extends BaseDomainException.Builder {

        private Builder(String errorCode, String message) {
            super(errorCode, message);
            // Agregar dominio automáticamente
            super.detail("domain", DOMAIN);
        }

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

        public OrderDomainException build() {
            return new OrderDomainException(
                getMessage(),
                getErrorCode(),
                getHttpStatus(),
                getDetails()
            );
        }
    }

    // Factory methods convenientes
    public static OrderDomainException orderNotFound(String orderId) {
        return OrderDomainException.builder("ORDER_NOT_FOUND", "Order not found")
            .httpStatus(HttpStatus.NOT_FOUND)
            .detail("orderId", orderId)
            .build();
    }

    public static OrderDomainException invalidOrderStatus(String orderId, String currentStatus, String attemptedStatus) {
        return OrderDomainException.builder("INVALID_ORDER_STATUS",
                "Cannot change order status from " + currentStatus + " to " + attemptedStatus)
            .httpStatus(HttpStatus.BAD_REQUEST)
            .detail("orderId", orderId)
            .detail("currentStatus", currentStatus)
            .detail("attemptedStatus", attemptedStatus)
            .build();
    }
}
```

**Uso de la excepción personalizada:**

```java
@Service
public class OrderService {

    public Order getOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw OrderDomainException.orderNotFound(orderId);
        }
        return order;
    }

    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = getOrder(orderId);

        if (!order.canTransitionTo(newStatus)) {
            throw OrderDomainException.invalidOrderStatus(
                orderId,
                order.getStatus().name(),
                newStatus.name()
            );
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
    }
}
```

#### Opción 3: Excepción de Validación Personalizada

```java
import com.company.exceptionhandling.starter.domain.BaseDomainException;
import com.company.exceptionhandling.starter.domain.ValidationException;
import org.springframework.http.HttpStatus;
import java.util.Map;

// Excepción personalizada para validaciones de negocio complejas
public class BusinessValidationException extends ValidationException {

    private static final String CATEGORY = "BUSINESS_VALIDATION";

    private BusinessValidationException(String message, String errorCode,
                                      HttpStatus httpStatus, Map<String, Object> details,
                                      Map<String, String> fieldErrors, List<String> globalErrors) {
        super(message, errorCode, httpStatus, details, fieldErrors, globalErrors);
    }

    // Builder personalizado
    public static Builder builder(String errorCode, String message) {
        return new Builder(errorCode, message);
    }

    public static class Builder extends ValidationException.Builder {

        private Builder(String errorCode, String message) {
            super(errorCode, message);
            // Override default status si es necesario
            super.httpStatus(HttpStatus.BAD_REQUEST);
        }

        @Override
        public Builder fieldError(String field, String message) {
            super.fieldError(field, message);
            return this;
        }

        @Override
        public Builder globalError(String message) {
            super.globalError(message);
            return this;
        }

        public BusinessValidationException build() {
            // Agregar categoría automáticamente
            super.detail("category", CATEGORY);

            // El padre ValidationException.Builder ya maneja fieldErrors y globalErrors
            Map<String, Object> allDetails = new HashMap<>(getDetails());
            allDetails.put("fieldErrors", Map.copyOf(new HashMap<>())); // Los fieldErrors se manejan en ValidationException.Builder
            allDetails.put("globalErrors", List.copyOf(new ArrayList<>())); // Los globalErrors se manejan en ValidationException.Builder
            allDetails.put("errorCount", 0);
            allDetails.put("category", CATEGORY);

            return new BusinessValidationException(
                getMessage(),
                getErrorCode(),
                getHttpStatus(),
                allDetails,
                new HashMap<>(), // fieldErrors
                new ArrayList<>()  // globalErrors
            );
        }
    }

    // Factory methods para escenarios comunes
    public static BusinessValidationException invalidOrderAmount(double amount, double minAmount) {
        return BusinessValidationException.builder("INVALID_ORDER_AMOUNT",
                "Order amount must be at least " + minAmount)
            .globalError("Order amount $" + amount + " is below minimum $" + minAmount)
            .detail("minimumAmount", minAmount)
            .detail("providedAmount", amount)
            .build();
    }

    public static BusinessValidationException duplicateItemsInOrder(List<String> duplicateProductIds) {
        return BusinessValidationException.builder("DUPLICATE_ORDER_ITEMS",
                "Order contains duplicate products")
            .globalError("The following products appear multiple times: " + String.join(", ", duplicateProductIds))
            .detail("duplicateProductIds", duplicateProductIds)
            .build();
    }
}
```

**Uso de la validación de negocio personalizada:**

```java
@Service
public class OrderValidationService {

    public void validateOrder(OrderRequest request) {
        List<String> errors = new ArrayList<>();

        // Validar monto mínimo
        if (request.getTotalAmount() < 10.0) {
            throw BusinessValidationException.invalidOrderAmount(
                request.getTotalAmount(), 10.0);
        }

        // Validar duplicados
        Set<String> productIds = new HashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (OrderItem item : request.getItems()) {
            if (!productIds.add(item.getProductId())) {
                duplicates.add(item.getProductId());
            }
        }

        if (!duplicates.isEmpty()) {
            throw BusinessValidationException.duplicateItemsInOrder(duplicates);
        }

        // Otras validaciones de negocio...
    }
}
```

## Uso Básico

### Lanzando Excepciones de Negocio

```java
import com.company.exceptionhandling.starter.domain.BusinessException;
import com.company.exceptionhandling.starter.errors.CommonErrorCodes;

public class UserService {

    public User findUser(String id) {
        if (userNotFound) {
            throw BusinessException.of(CommonErrorCodes.RESOURCE_NOT_FOUND)
                .detail("userId", id)
                .detail("resourceType", "User");
        }
    }

    public void createUser(User user) {
        if (userExists) {
            throw BusinessException.of(CommonErrorCodes.RESOURCE_ALREADY_EXISTS)
                .detail("userId", user.getId());
        }
    }
}
```

### Excepciones de Validación

```java
import com.company.exceptionhandling.starter.domain.ValidationException;

public class UserController {

    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        // Validación automática de excepciones
        return userService.createUser(user);
    }

    public void customValidation() {
        throw ValidationException.fieldError("email", "Formato de email inválido")
            .fieldError("age", "Debe ser mayor de 18 años");
    }
}
```

### Códigos de Error Personalizados

```java
import com.company.exceptionhandling.starter.domain.ErrorCode;
import com.company.exceptionhandling.starter.domain.BusinessException;

public interface MyErrorCodes {
    ErrorCode INSUFFICIENT_PERMISSIONS = ErrorCode.of(
        "INSUFFICIENT_PERMISSIONS",
        "Usuario no tiene permisos requeridos",
        HttpStatus.FORBIDDEN
    );
}

public class SecurityService {
    public void checkPermission() {
        throw BusinessException.of(MyErrorCodes.INSUFFICIENT_PERMISSIONS)
            .detail("requiredRole", "ADMIN");
    }
}
```

## Formato de Respuesta de Error

Todas las respuestas de error retornan respuestas ProblemDetail (RFC 9457):

```json
{
  "type": "https://api.mycompany.com/errors/RESOURCE_NOT_FOUND",
  "title": "Resource not found",
  "status": 404,
  "detail": "The requested resource was not found",
  "timestamp": "2023-12-08T12:34:56.789Z",
  "traceId": "abc123def456",
  "spanId": "def789",
  "errorCode": "RESOURCE_NOT_FOUND",
  "userId": "12345",
  "resourceType": "User"
}
```

## Logging

El starter proporciona logging estructurado con:
- Contexto MDC para rastreo de errores
- IDs de trace y span para tracing distribuido
- Categorización de errores (BUSINESS, VALIDATION, GENERIC)
- Niveles de log configurables

Ejemplo de salida de log:
```
ERROR [traceId=abc123, spanId=def789, error.code=RESOURCE_NOT_FOUND, error.http_status=404] Domain exception occurred
```

## Dependencias

- Spring Boot 3.2+
- Java 21+
- Spring Web
- Spring Validation
- Micrometer Tracing
- Logstash Logback Encoder

## Construcción

```bash
mvn clean install
```

## Contribución

1. Fork el repositorio
2. Crear rama de feature
3. Agregar tests para nueva funcionalidad
4. Asegurar que todos los tests pasen
5. Enviar pull request

## Licencia

Este proyecto está licenciado bajo la Licencia MIT.
