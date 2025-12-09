package com.company.exceptionhandling.starter.config;

import com.company.exceptionhandling.starter.handler.GlobalExceptionHandler;
import com.company.exceptionhandling.starter.properties.ExceptionHandlingProperties;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the exception handling starter.
 * Provides automatic exception handling with ProblemDetail responses.
 *
 * This configuration automatically registers a GlobalExceptionHandler
 * that can be overridden by applications using @ConditionalOnMissingBean.
 */
@AutoConfiguration
@EnableConfigurationProperties(ExceptionHandlingProperties.class)
public class ExceptionHandlingAutoConfiguration {

    /**
     * Automatically registers the global exception handler.
     * Applications can override this by providing their own GlobalExceptionHandler bean.
     */
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler(
            ExceptionHandlingProperties properties,
            Tracer tracer) {
        return new GlobalExceptionHandler(properties, tracer);
    }
}
