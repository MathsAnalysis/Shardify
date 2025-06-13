package it.mathsanalysis.load.spi.cache.structure;

import it.mathsanalysis.load.spi.cache.event.CacheEventListener;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * High-performance cache interface with advanced features
 * Thread-safe with minimal overhead and ultra-fast operations
 */
public interface Cache<K, V> {

    // Basic Operations

    /**
     * Get value by key
     *
     * @param key Cache key
     * @return Optional containing value if found
     */
    Optional<V> get(K key);

    /**
     * Get value by key with loader function if not present
     *
     * @param key    Cache key
     * @param loader Function to load value if not in cache
     * @return Value from cache or loaded by function
     */
    V get(K key, Function<K, V> loader);

    /**
     * Put value in cache
     *
     * @param key   Cache key
     * @param value Value to cache
     */
    void put(K key, V value);

    /**
     * Put value in cache with custom TTL
     *
     * @param key   Cache key
     * @param value Value to cache
     * @param ttl   Time to live
     */
    void put(K key, V value, Duration ttl);

    /**
     * Put value only if key is not present
     *
     * @param key   Cache key
     * @param value Value to cache
     * @return Previous value if present, null otherwise
     */
    V putIfAbsent(K key, V value);

    /**
     * Remove value by key
     *
     * @param key Cache key
     * @return Previous value if present
     */
    Optional<V> remove(K key);

    /**
     * Check if key exists in cache
     *
     * @param key Cache key
     * @return true if key exists
     */
    boolean containsKey(K key);

    /**
     * Get all cached entries
     *
     * @return Map of all cached entries
     */
    Map<K, V> asMap();

    /**
     * Clear all entries
     */
    void clear();

    /**
     * Get cache size
     *
     * @return Number of entries in cache
     */
    long size();

    /**
     * Check if cache is empty
     *
     * @return true if cache has no entries
     */
    boolean isEmpty();

    // Bulk Operations

    /**
     * Get multiple values
     *
     * @param keys Set of keys
     * @return Map of found key-value pairs
     */
    Map<K, V> getAll(Set<K> keys);

    /**
     * Put multiple values
     *
     * @param map Map of key-value pairs to cache
     */
    void putAll(Map<K, V> map);

    /**
     * Remove multiple keys
     *
     * @param keys Set of keys to remove
     */
    void removeAll(Set<K> keys);

    // Async Operations

    /**
     * Async get operation
     *
     * @param key Cache key
     * @return CompletableFuture with optional value
     */
    CompletableFuture<Optional<V>> getAsync(K key);

    /**
     * Async put operation
     *
     * @param key   Cache key
     * @param value Value to cache
     * @return CompletableFuture that completes when put is done
     */
    CompletableFuture<Void> putAsync(K key, V value);

    /**
     * Async remove operation
     *
     * @param key Cache key
     * @return CompletableFuture with optional previous value
     */
    CompletableFuture<Optional<V>> removeAsync(K key);

    // Cache Statistics

    /**
     * Get cache statistics
     *
     * @return Cache performance metrics
     */
    CacheStats getStats();

    /**
     * Reset cache statistics
     */
    void resetStats();

    // Cache Management

    /**
     * Get cache name
     *
     * @return Cache name
     */
    String getName();

    /**
     * Get cache configuration
     *
     * @return Cache configuration
     */
    CacheConfiguration getConfiguration();

    /**
     * Update cache configuration (if supported)
     *
     * @param newConfig New configuration
     * @return true if configuration was updated
     */
    boolean updateConfiguration(CacheConfiguration newConfig);

    /**
     * Cleanup expired entries
     */
    void cleanUp();

    /**
     * Close cache and release resources
     */
    void close();

    // Eviction Control

    /**
     * Manually evict specific key
     *
     * @param key Key to evict
     */
    void evict(K key);

    /**
     * Evict all entries matching predicate
     *
     * @param predicate Predicate to test keys
     */
    void evictAll(java.util.function.Predicate<K> predicate);

    /**
     * Get estimated size in bytes (if supported)
     *
     * @return Estimated memory usage in bytes
     */
    long estimatedSize();

    // Listeners and Events

    /**
     * Add cache event listener
     *
     * @param listener Event listener
     */
    void addListener(CacheEventListener<K, V> listener);

    /**
     * Remove cache event listener
     *
     * @param listener Event listener to remove
     */
    void removeListener(CacheEventListener<K, V> listener);
}