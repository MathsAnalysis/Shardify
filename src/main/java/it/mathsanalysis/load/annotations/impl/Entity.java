package it.mathsanalysis.load.annotations.impl;

import java.lang.annotation.*;

/**
 * Mark a class as an entity for persistence
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
    String name() default ""; // Name table in database
    boolean cacheable() default true;
}
