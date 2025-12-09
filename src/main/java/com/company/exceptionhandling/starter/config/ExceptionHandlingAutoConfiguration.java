package com.company.exceptionhandling.starter.config;

import com.company.exceptionhandling.starter.handler.StarterGlobalExceptionHandler;
import com.company.exceptionhandling.starter.properties.ExceptionHandlingProperties;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the exception handling starter.
 * Provides all necessary beans for modern exception handling.
 */
@AutoConfiguration
@EnableConfigurationProperties(ExceptionHandlingProperties.class)
public class ExceptionHandlingAutoConfiguration {

    /**
     * Provides the modern GlobalExceptionHandler as a Spring bean.
     */
    @Bean
    public StarterGlobalExceptionHandler starterGlobalExceptionHandler(
            ExceptionHandlingProperties properties,
            @Autowired(required = false) Tracer tracer) {
        return new StarterGlobalExceptionHandler(properties, tracer);
    }
}
