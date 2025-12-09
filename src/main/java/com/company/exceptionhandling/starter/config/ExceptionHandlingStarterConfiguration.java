package com.company.exceptionhandling.starter.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the Exception Handling Starter.
 * This is imported by the @ExceptionHandlingStarter annotation.
 * In practice, the auto-configuration handles everything, but this
 * provides a way for users to explicitly enable the starter.
 */
@Configuration
public class ExceptionHandlingStarterConfiguration {
    // This class can be empty since auto-configuration does the work
    // But it provides a hook for future manual configuration if needed
}
