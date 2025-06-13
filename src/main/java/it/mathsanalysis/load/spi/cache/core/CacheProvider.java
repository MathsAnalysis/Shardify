package it.mathsanalysis.load.spi.cache.core;

import it.mathsanalysis.load.spi.cache.structure.Cache;
import it.mathsanalysis.load.spi.cache.structure.CacheConfiguration;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cache provider interface
 */
public interface CacheProvider {
    
    /**
     * Create new cache instance
     * @param configuration Cache configuration
     * @return New cache instance
     */
    <K, V> Cache<K, V> createCache(CacheConfiguration configuration);
    
    /**
     * Get cache by name
     * @param name Cache name
     * @return Cache instance if exists
     */
    <K, V> Optional<Cache<K, V>> getCache(String name);
    
    /**
     * Get all cache names
     * @return Set of cache names
     */
    Set<String> getCacheNames();
    
    /**
     * Destroy cache by name
     * @param name Cache name
     */
    void destroyCache(String name);
    
    /**
     * Close all caches and cleanup
     */
    void close();
    
    /**
     * Get provider name
     * @return Provider name (e.g., "Caffeine", "Java")
     */
    String getProviderName();
    
    /**
     * Get provider statistics
     * @return Provider-level statistics
     */
    Map<String, Object> getProviderStats();
}
