package it.mathsanalysis.load.spi.cache.core;

import it.mathsanalysis.load.spi.cache.structure.Cache;
import it.mathsanalysis.load.spi.cache.structure.CacheConfiguration;
import it.mathsanalysis.load.spi.cache.structure.CacheStats;

import java.util.Map;

/**
 * Cache manager interface
 */
public interface CacheManager {
    
    /**
     * Get cache by name, create if not exists
     * @param name Cache name
     * @param configuration Cache configuration
     * @return Cache instance
     */
    <K, V> Cache<K, V> getCache(String name, CacheConfiguration configuration);
    
    /**
     * Get cache by name with default configuration
     * @param name Cache name
     * @return Cache instance
     */
    <K, V> Cache<K, V> getCache(String name);
    
    /**
     * Get all caches
     * @return Map of cache name to cache instance
     */
    Map<String, Cache<?, ?>> getAllCaches();
    
    /**
     * Remove cache by name
     * @param name Cache name
     */
    void removeCache(String name);
    
    /**
     * Clear all caches
     */
    void clearAll();
    
    /**
     * Get cache statistics for all caches
     * @return Map of cache name to statistics
     */
    Map<String, CacheStats> getAllStats();
    
    /**
     * Set global cache configuration
     * @param configuration Global configuration
     */
    void setGlobalConfiguration(CacheConfiguration configuration);
    
    /**
     * Get global cache configuration
     * @return Global configuration
     */
    CacheConfiguration getGlobalConfiguration();
    
    /**
     * Close cache manager and all caches
     */
    void close();
}