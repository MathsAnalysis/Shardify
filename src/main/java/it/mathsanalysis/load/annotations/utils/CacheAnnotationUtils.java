package it.mathsanalysis.load.annotations.utils;

import it.mathsanalysis.load.annotations.impl.CacheConfig;
import it.mathsanalysis.load.annotations.impl.CacheEvict;
import it.mathsanalysis.load.annotations.impl.CachePut;
import it.mathsanalysis.load.annotations.impl.Cacheable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cache annotation utilities
 */
public final class CacheAnnotationUtils {

    private CacheAnnotationUtils() {
    }

    /**
     * Check if class has any cache annotations
     */
    public static boolean hasCacheAnnotations(Class<?> clazz) {
        if (clazz.isAnnotationPresent(CacheConfig.class)) {
            return true;
        }

        for (var method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Cacheable.class) ||
                    method.isAnnotationPresent(CachePut.class) ||
                    method.isAnnotationPresent(CacheEvict.class)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get all cache names used in a class
     */
    public static Set<String> getCacheNames(Class<?> clazz) {
        var cacheNames = new HashSet<String>();

        var cacheConfig = clazz.getAnnotation(CacheConfig.class);
        if (cacheConfig != null) {
            cacheNames.add(cacheConfig.region());
        }

        for (var method : clazz.getDeclaredMethods()) {
            var cacheable = method.getAnnotation(Cacheable.class);
            if (cacheable != null) {
                cacheNames.add(cacheable.value());
            }

            var cachePut = method.getAnnotation(CachePut.class);
            if (cachePut != null) {
                cacheNames.add(cachePut.value());
            }

            var cacheEvict = method.getAnnotation(CacheEvict.class);
            if (cacheEvict != null) {
                cacheNames.add(cacheEvict.value());
            }
        }

        return cacheNames;
    }

    /**
     * Validate cache annotations on a class
     */
    public static List<String> validateCacheAnnotations(Class<?> clazz) {
        var errors = new ArrayList<String>();

        for (var method : clazz.getDeclaredMethods()) {
            var annotations = 0;

            if (method.isAnnotationPresent(Cacheable.class)) annotations++;
            if (method.isAnnotationPresent(CachePut.class)) annotations++;
            if (method.isAnnotationPresent(CacheEvict.class)) annotations++;

            if (annotations > 1) {
                errors.add("Method " + method.getName() + " has multiple cache annotations");
            }

            // Validate method return type for @Cacheable
            if (method.isAnnotationPresent(Cacheable.class) && method.getReturnType() == void.class) {
                errors.add("@Cacheable method " + method.getName() + " cannot have void return type");
            }
        }

        return errors;
    }
}
