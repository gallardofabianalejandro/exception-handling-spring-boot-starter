package com.company.exceptionhandling.starter;

import com.company.exceptionhandling.starter.config.ExceptionHandlingAutoConfiguration;
import com.company.exceptionhandling.starter.domain.BusinessException;
import com.company.exceptionhandling.starter.errors.CommonErrorCodes;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration test for the exception handling starter.
 * Tests that the auto-configuration works and exceptions are handled correctly.
 */
class ExceptionHandlingStarterTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ExceptionHandlingAutoConfiguration.class))
        .withPropertyValues(
            "app.exception.include-stack-trace=true",
            "app.exception.base-error-uri=https://api.test.com/errors"
        );

    @Test
    void contextLoads() {
        // Test that Spring context loads with our auto-configuration
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ExceptionHandlingAutoConfiguration.class);
            assertThat(context).hasSingleBean(com.company.exceptionhandling.starter.handler.GlobalExceptionHandler.class);
        });
    }

    @Test
    void businessExceptionCreation() {
        // Test basic exception creation
        BusinessException exception = BusinessException.builder(CommonErrorCodes.RESOURCE_NOT_FOUND)
            .detail("resourceId", "123")
            .detail("resourceType", "User")
            .build();

        assertThat(exception.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(exception.getHttpStatus().value()).isEqualTo(404);
        assertThat(exception.getDetails()).containsKey("resourceId");
        assertThat(exception.getDetails().get("resourceId")).isEqualTo("123");
    }

    @Test
    void errorCodeCreation() {
        // Test error code creation
        var errorCode = CommonErrorCodes.RESOURCE_NOT_FOUND;
        assertThat(errorCode.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(errorCode.defaultMessage()).isEqualTo("Resource not found");
        assertThat(errorCode.httpStatus().value()).isEqualTo(404);
    }
}
