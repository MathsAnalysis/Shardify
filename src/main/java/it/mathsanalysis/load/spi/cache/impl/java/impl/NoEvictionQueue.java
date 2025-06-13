package it.mathsanalysis.load.spi.cache.impl.java.impl;

import it.mathsanalysis.load.spi.cache.impl.java.entry.CacheEntry;
import it.mathsanalysis.load.spi.cache.impl.java.structure.EvictionQueue;

public class NoEvictionQueue implements EvictionQueue {

    @Override
    public void onPut(Object key, CacheEntry entry) {
    }

    @Override
    public void onAccess(Object key, CacheEntry entry) {
    }

    @Override
    public void onRemove(Object key, CacheEntry entry) {
    }

    @Override
    public Object selectVictim() {
        return null;
    }

    @Override
    public void clear() {
    }
}