package it.mathsanalysis.load.annotations.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables caching for methods or classes
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {
    String value() default "default"; // Cache name
    String key() default ""; // SpEL expression for key generation
    long ttl() default 3600; // TTL in seconds
    boolean async() default false;
}
