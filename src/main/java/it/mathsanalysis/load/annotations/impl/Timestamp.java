package it.mathsanalysis.load.annotations.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * field annotation for timestamp fields
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timestamp {
    Type value() default Type.UPDATED;
    
    enum Type {
        CREATED, UPDATED, BOTH
    }
}

