package dev.abstratium.abstoggle.boundary.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * Annotation to mark REST resource methods that require a change note header.
 * The header "X-Change-Note" must be present and non-empty, or a validation error (400) will be returned.
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresChangeNote {
}
