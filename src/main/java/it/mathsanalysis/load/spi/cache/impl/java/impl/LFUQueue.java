package it.mathsanalysis.load.spi.cache.impl.java.impl;

import it.mathsanalysis.load.spi.cache.impl.java.entry.CacheEntry;
import it.mathsanalysis.load.spi.cache.impl.java.structure.EvictionQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// All'interno di JavaPureCache<K, V>
public class LFUQueue implements EvictionQueue {

    private final ConcurrentHashMap<Object, Long> frequencies = new ConcurrentHashMap<>();

    @Override
    public void onPut(Object key, CacheEntry entry) {
        frequencies.put(key, 1L);
    }

    @Override
    public void onAccess(Object key, CacheEntry entry) {
        frequencies.merge(key, 1L, Long::sum);
    }

    @Override
    public void onRemove(Object key, CacheEntry entry) {
        frequencies.remove(key);
    }

    @Override
    public Object selectVictim() {
        return frequencies.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public void clear() {
        frequencies.clear();
    }
}