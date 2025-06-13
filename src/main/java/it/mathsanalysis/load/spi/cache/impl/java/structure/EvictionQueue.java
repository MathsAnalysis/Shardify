package it.mathsanalysis.load.spi.cache.impl.java.structure;


import it.mathsanalysis.load.spi.cache.impl.java.entry.CacheEntry;

/**
 * Eviction queue interface
 */
public interface EvictionQueue<K, V> {

    void onPut(K key, CacheEntry<V> entry);
    void onAccess(K key, CacheEntry<V> entry);
    void onRemove(K key, CacheEntry<V> entry);
    K selectVictim();
    void clear();
}