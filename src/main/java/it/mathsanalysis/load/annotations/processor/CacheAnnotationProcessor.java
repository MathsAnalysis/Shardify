package it.mathsanalysis.load.annotations.processor;

import it.mathsanalysis.load.annotations.impl.CacheConfig;
import it.mathsanalysis.load.annotations.impl.CacheEvict;
import it.mathsanalysis.load.annotations.impl.CachePut;
import it.mathsanalysis.load.annotations.impl.Cacheable;
import it.mathsanalysis.load.spi.cache.core.CacheManager;
import it.mathsanalysis.load.spi.cache.core.CacheManagers;
import it.mathsanalysis.load.spi.cache.structure.Cache;
import it.mathsanalysis.load.spi.cache.structure.CacheConfiguration;
import it.mathsanalysis.load.spi.cache.type.EvictionPolicy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime cache annotation processor using dynamic proxies
 * Handles @Cacheable, @CacheEvict, @CachePut annotations
 */
public final class CacheAnnotationProcessor {
    
    private final Map<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();
    private final CacheManager cacheManager;
    
    public CacheAnnotationProcessor() {
        this.cacheManager = CacheManagers.getDefault();
    }
    
    public CacheAnnotationProcessor(CacheManager cacheManager) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "Cache manager cannot be null");
    }
    
    /**
     * Create cached proxy for an object
     * @param target Target object to wrap
     * @param targetInterface Interface to proxy
     * @return Proxied object with cache annotations support
     */
    @SuppressWarnings("unchecked")
    public <T> T createCachedProxy(T target, Class<T> targetInterface) {
        Objects.requireNonNull(target, "Target cannot be null");
        Objects.requireNonNull(targetInterface, "Target interface cannot be null");
        
        if (!targetInterface.isInterface()) {
            throw new IllegalArgumentException("Target interface must be an interface");
        }
        
        return (T) Proxy.newProxyInstance(
            targetInterface.getClassLoader(),
            new Class<?>[]{targetInterface},
            new CacheInvocationHandler(target, this)
        );
    }
    
    /**
     * Process @Cacheable annotation
     */
    Object processCacheable(Method method, Object[] args, CacheableInvocation invocation) throws Throwable {
        var cacheable = method.getAnnotation(Cacheable.class);
        if (cacheable == null) {
            return invocation.proceed();
        }
        
        var cache = getOrCreateCache(cacheable.value(), Duration.ofSeconds(cacheable.ttl()));
        var key = generateKey(method, args, cacheable.key());
        
        var cachedValue = cache.get(key);
        if (cachedValue.isPresent()) {
            return cachedValue.get();
        }
        
        var result = invocation.proceed();
        if (result != null) {
            if (cacheable.async()) {
                cache.putAsync(key, result);
            } else {
                cache.put(key, result, Duration.ofSeconds(cacheable.ttl()));
            }
        }
        
        return result;
    }
    
    /**
     * Process @CachePut annotation
     */
    Object processCachePut(Method method, Object[] args, CacheableInvocation invocation) throws Throwable {
        var cachePut = method.getAnnotation(CachePut.class);
        if (cachePut == null) {
            return invocation.proceed();
        }
        
        var result = invocation.proceed();
        
        if (result != null) {
            var cache = getOrCreateCache(cachePut.value(), Duration.ofSeconds(cachePut.ttl()));
            var key = generateKey(method, args, cachePut.key());
            cache.put(key, result, Duration.ofSeconds(cachePut.ttl()));
        }
        
        return result;
    }
    
    /**
     * Process @CacheEvict annotation
     */
    Object processCacheEvict(Method method, Object[] args, CacheableInvocation invocation) throws Throwable {
        var cacheEvict = method.getAnnotation(CacheEvict.class);
        if (cacheEvict == null) {
            return invocation.proceed();
        }
        
        var cache = getOrCreateCache(cacheEvict.value(), Duration.ofHours(1));
        
        if (cacheEvict.beforeInvocation()) {
            performEviction(cache, method, args, cacheEvict);
        }
        
        var result = invocation.proceed();
        
        if (!cacheEvict.beforeInvocation()) {
            performEviction(cache, method, args, cacheEvict);
        }
        
        return result;
    }
    
    /**
     * Process class-level @CacheConfig annotation
     */
    void processCacheConfig(Class<?> targetClass) {
        var cacheConfig = targetClass.getAnnotation(CacheConfig.class);
        if (cacheConfig != null) {
            var configuration = CacheConfiguration.builder()
                .name(cacheConfig.region())
                .defaultTtl(Duration.ofSeconds(cacheConfig.ttl()))
                .maxSize(cacheConfig.maxSize())
                .evictionPolicy(mapEvictionStrategy(cacheConfig.strategy()))
                .build();
            
            cacheManager.setGlobalConfiguration(configuration);
        }
    }
    

    private Cache<String, Object> getOrCreateCache(String cacheName, Duration ttl) {
        return caches.computeIfAbsent(cacheName, name -> {
            var config = CacheConfiguration.builder()
                .name(name)
                .defaultTtl(ttl)
                .recordStats(true)
                .allowNullValues(false)
                .build();
            
            return cacheManager.getCache(name, config);
        });
    }
    
    private String generateKey(Method method, Object[] args, String keyExpression) {
        if (keyExpression != null && !keyExpression.isEmpty()) {
            return evaluateKeyExpression(keyExpression, method, args);
        }
        
        // Default key generation
        var keyBuilder = new StringBuilder();
        keyBuilder.append(method.getDeclaringClass().getSimpleName())
                 .append(".")
                 .append(method.getName());
        
        if (args != null && args.length > 0) {
            keyBuilder.append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) keyBuilder.append(",");
                keyBuilder.append(args[i] != null ? args[i].toString() : "null");
            }
            keyBuilder.append(")");
        }
        
        return keyBuilder.toString();
    }
    
    private String evaluateKeyExpression(String expression, Method method, Object[] args) {
        // Simplified SpEL-like expression evaluation
        if (expression.startsWith("#") && expression.length() > 1) {
            var paramName = expression.substring(1);
            
            // Try to match parameter by name (simplified)
            var paramNames = getParameterNames(method);
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                if (paramNames[i].equals(paramName)) {
                    return args[i] != null ? args[i].toString() : "null";
                }
            }
        }
        
        return expression;
    }
    
    private String[] getParameterNames(Method method) {
        // Simplified parameter name extraction
        // In real implementation, would use reflection or parameter name discovery
        var paramCount = method.getParameterCount();
        var names = new String[paramCount];
        for (int i = 0; i < paramCount; i++) {
            names[i] = "arg" + i;
        }
        return names;
    }
    
    private void performEviction(Cache<String, Object> cache, Method method, Object[] args, CacheEvict cacheEvict) {
        if (cacheEvict.allEntries()) {
            cache.clear();
        } else {
            var key = generateKey(method, args, cacheEvict.key());
            cache.remove(key);
        }
    }
    
    private EvictionPolicy mapEvictionStrategy(CacheConfig.Strategy strategy) {
        return switch (strategy) {
            case LRU -> EvictionPolicy.LRU;
            case LFU -> EvictionPolicy.LFU;
            case FIFO -> EvictionPolicy.FIFO;
            case RANDOM -> EvictionPolicy.RANDOM;
        };
    }
    
    /**
     * Functional interface for method invocation
     */
    @FunctionalInterface
    interface CacheableInvocation {
        Object proceed() throws Throwable;
    }
    
    /**
     * Dynamic proxy invocation handler
     */
    private static class CacheInvocationHandler implements java.lang.reflect.InvocationHandler {
        private final Object target;
        private final CacheAnnotationProcessor processor;
        
        CacheInvocationHandler(Object target, CacheAnnotationProcessor processor) {
            this.target = target;
            this.processor = processor;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Process class-level cache config
            processor.processCacheConfig(target.getClass());
            
            // Create invocation wrapper
            CacheableInvocation invocation = () -> method.invoke(target, args);
            
            // Check for cache annotations in order of precedence
            if (method.isAnnotationPresent(CacheEvict.class)) {
                return processor.processCacheEvict(method, args, invocation);
            } else if (method.isAnnotationPresent(CachePut.class)) {
                return processor.processCachePut(method, args, invocation);
            } else if (method.isAnnotationPresent(Cacheable.class)) {
                return processor.processCacheable(method, args, invocation);
            } else {
                return invocation.proceed();
            }
        }
    }
}

