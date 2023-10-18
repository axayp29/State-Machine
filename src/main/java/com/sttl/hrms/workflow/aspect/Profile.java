package com.sttl.hrms.workflow.aspect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * Use this annotation on methods to log the time taken for execution.
 */
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Profile {

}
