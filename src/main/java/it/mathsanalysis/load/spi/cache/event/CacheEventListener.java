package it.mathsanalysis.load.spi.cache.event;

import it.mathsanalysis.load.spi.cache.type.RemovalCause;

/**
 * Cache event listener interface
 */
public interface CacheEventListener<K, V> {
    
    /**
     * Called when entry is added to cache
     */
    default void onPut(K key, V value) {}
    
    /**
     * Called when entry is retrieved from cache
     */
    default void onGet(K key, V value, boolean isHit) {}
    
    /**
     * Called when entry is removed from cache
     */
    default void onRemove(K key, V value, RemovalCause cause) {}
    
    /**
     * Called when entry is evicted from cache
     */
    default void onEvict(K key, V value, RemovalCause cause) {}
    
    /**
     * Called when cache is cleared
     */
    default void onClear() {}
}