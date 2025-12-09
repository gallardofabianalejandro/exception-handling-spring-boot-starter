package com.company.exceptionhandling.starter;

import com.company.exceptionhandling.starter.config.ExceptionHandlingStarterConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable the Exception Handling Starter.
 * This annotation is optional since auto-configuration is enabled by default,
 * but can be used for explicit configuration.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ExceptionHandlingStarterConfiguration.class)
public @interface ExceptionHandlingStarter {
}
