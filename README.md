# Exception Handling Spring Boot Starter

A modern Spring Boot starter for comprehensive exception handling with ProblemDetail support (RFC 7807), structured logging, and observability features.

## Features

- **ProblemDetail Support**: Modern error responses following RFC 7807
- **Structured Logging**: Enterprise-ready logging with MDC context and trace/span IDs
- **Domain Exceptions**: Type-safe exception hierarchy with builder pattern
- **Validation Support**: Integration with Spring Validation and custom validation exceptions
- **Observability**: Micrometer tracing integration for distributed tracing
- **Configurable**: Extensive configuration options via application properties
- **Java 21**: Built with modern Java features and records

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.company</groupId>
    <artifactId>exception-handling-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Auto-Configuration

The starter is automatically configured when added to your classpath. No additional setup required!

### 3. Optional: Enable Explicitly

```java
@SpringBootApplication
@EnableExceptionHandlingStarter
public class MyApplication {
    // ...
}
```

## Usage

### Throwing Business Exceptions

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

### Validation Exceptions

```java
import com.company.exceptionhandling.starter.domain.ValidationException;

public class UserController {

    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        // Automatic validation exception handling
        return userService.createUser(user);
    }

    public void customValidation() {
        throw ValidationException.fieldError("email", "Invalid email format")
            .fieldError("age", "Must be 18 or older");
    }
}
```

### Custom Error Codes

```java
import com.company.exceptionhandling.starter.domain.ErrorCode;
import com.company.exceptionhandling.starter.domain.BusinessException;

public interface MyErrorCodes {
    ErrorCode INSUFFICIENT_PERMISSIONS = ErrorCode.of(
        "INSUFFICIENT_PERMISSIONS",
        "User does not have required permissions",
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

## Configuration

Configure the starter via `application.properties` or `application.yml`:

```properties
# Exception handling configuration
app.exception.include-stack-trace=false
app.exception.include-cause=false
app.exception.log-level=ERROR
app.exception.sensitive-fields=user.password,account.balance
app.exception.expose-error-codes=true
app.exception.base-error-uri=https://api.mycompany.com/errors
```

### Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `app.exception.include-stack-trace` | `false` | Include stack traces in error responses |
| `app.exception.include-cause` | `false` | Include exception causes |
| `app.exception.log-level` | `ERROR` | Log level for exceptions (ERROR, WARN, INFO, OFF) |
| `app.exception.sensitive-fields` | `[]` | List of sensitive field names to mask in logs |
| `app.exception.expose-error-codes` | `true` | Whether to expose internal error codes |
| `app.exception.base-error-uri` | `https://api.company.com/errors` | Base URI for error type links |

## Error Response Format

All errors return ProblemDetail responses (RFC 7807):

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

## Exception Hierarchy

```
BaseDomainException (abstract)
├── BusinessException
└── ValidationException
```

### BaseDomainException

Base class for all domain exceptions with:
- Error code and HTTP status
- Additional details map
- Builder pattern for fluent API
- Convenience methods for error categorization

### BusinessException

For business logic violations:
- Factory methods for common scenarios
- Support for custom error codes
- Additional business context in details

### ValidationException

For validation errors:
- Integration with Spring Validation
- Field-level and global error support
- Automatic conversion from BindingResult

## Logging

The starter provides structured logging with:
- MDC context for error tracking
- Trace and span IDs for distributed tracing
- Error categorization (BUSINESS, VALIDATION, GENERIC)
- Configurable log levels

Example log output:
```
ERROR [traceId=abc123, spanId=def789, error.code=RESOURCE_NOT_FOUND, error.http_status=404] Domain exception occurred
```

## Dependencies

- Spring Boot 3.2+
- Java 21+
- Spring Web
- Spring Validation
- Micrometer Tracing
- Logstash Logback Encoder

## Building

```bash
mvn clean install
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License.
