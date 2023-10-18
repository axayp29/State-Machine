package com.sttl.hrms.workflow.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Profiling aspect that logs the time taken.
 */
@Aspect
@Component
@Slf4j
public class ProfilingAspect {

    /**
     * For all jpa repository methods, call logging method
     */
    @Around("this(org.springframework.data.jpa.repository.JpaRepository)")
    public Object databaseCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        return loggingMethod(joinPoint, "DB Call");
    }

    /**
     * For all methods that are annotated with @Profile, call logging method
     */
    @Around("@annotation(com.sttl.hrms.workflow.aspect.Profile)")
    public Object annotatedMethodCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        return loggingMethod(joinPoint, "Annotated Method");
    }

    /**
     * For all public methods in the controllers present in the package "com.sttl.hrms.workflow.resource", call logging method.
     */
    @Around("execution(public * com.sttl.hrms.workflow.resource.*.*(..))")
    public Object restControllerCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        return loggingMethod(joinPoint, "Rest API Call");
    }

    /**
     * Method to log the time taken for the execution of the methods around eligible pointcuts.
     */
    public static Object loggingMethod(ProceedingJoinPoint joinPoint, String source) throws Throwable {
        final String methodName = joinPoint.getSignature().toShortString();
        final double startTime = Instant.now().toEpochMilli();
        double duration;
        final Object returnValue;
        String message = "PROFILING {}: method={} completed in {} seconds";
        try {
            returnValue = joinPoint.proceed();
        } catch (Throwable ex) {
            duration = (Instant.now().toEpochMilli() - startTime) / 1000;
            if (duration > 5)
                log.debug(message + " with exception", source, methodName, String.format("%.3f", duration));
            else
                log.trace(message + " with exception", source, methodName, String.format("%.3f", duration));

            throw ex;
        }
        duration = (Instant.now().toEpochMilli() - startTime) / 1000;
        if (duration > 5)
            log.debug(message, source, methodName, String.format("%.3f", duration));
        else
            log.trace(message, source, methodName, String.format("%.3f", duration));
        return returnValue;
    }

}
