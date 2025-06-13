package it.mathsanalysis.load.spi.cache.impl.java.impl;

import it.mathsanalysis.load.spi.cache.impl.java.entry.CacheEntry;
import it.mathsanalysis.load.spi.cache.impl.java.structure.EvictionQueue;

import java.util.LinkedHashMap;

public class LRUQueue implements EvictionQueue {

    private final LinkedHashMap<Object, CacheEntry> accessOrder = new LinkedHashMap<>(16, 0.75f, true);
    private final Object lock = new Object();

    @Override
    public void onAccess(Object key, CacheEntry entry) {
        synchronized (lock) {
            accessOrder.put(key, entry);
        }
    }

    @Override
    public void onRemove(Object key, CacheEntry entry) {
        synchronized (lock) {
            accessOrder.remove(key);
        }
    }

    @Override
    public void onPut(Object key, CacheEntry entry) {
        synchronized (lock) {
            accessOrder.put(key, entry);
        }
    }

    @Override
    public Object selectVictim() {
        synchronized (lock) {
            return accessOrder.isEmpty() ? null : accessOrder.keySet().iterator().next();
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            accessOrder.clear();
        }
    }
}
