package it.mathsanalysis.load.annotations.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Config for caching behavior of a class
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheConfig {
    String region() default "default";
    long ttl() default 3600;
    int maxSize() default 1000;
    Strategy strategy() default Strategy.LRU;
    
    enum Strategy {
        LRU, LFU, FIFO, RANDOM
    }
}
