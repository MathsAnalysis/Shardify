package it.mathsanalysis.load.annotations.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Config for fields in document-based databases
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Field {
    String name() default "";
    boolean indexed() default false;
    boolean unique() default false;
    boolean sparse() default false;
}