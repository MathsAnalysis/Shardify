package it.mathsanalysis.load.annotations.factory;

import it.mathsanalysis.load.annotations.processor.CacheAnnotationProcessor;
import it.mathsanalysis.load.annotations.utils.CacheAnnotationUtils;
import it.mathsanalysis.load.spi.cache.core.CacheManager;

/**
 * Cache annotation factory for creating cached proxies
 */
public final class CacheAnnotationFactory {
    private static final CacheAnnotationProcessor DEFAULT_PROCESSOR = new CacheAnnotationProcessor();

    private CacheAnnotationFactory() {
    }

    /**
     * Create cached proxy using default processor
     */
    public static <T> T createCachedProxy(T target, Class<T> targetInterface) {
        return DEFAULT_PROCESSOR.createCachedProxy(target, targetInterface);
    }

    /**
     * Create cached proxy using custom cache manager
     */
    public static <T> T createCachedProxy(T target, Class<T> targetInterface, CacheManager cacheManager) {
        var processor = new CacheAnnotationProcessor(cacheManager);
        return processor.createCachedProxy(target, targetInterface);
    }

    /**
     * Check if target needs caching proxy
     */
    public static boolean needsCachingProxy(Class<?> targetClass) {
        return CacheAnnotationUtils.hasCacheAnnotations(targetClass);
    }
}
