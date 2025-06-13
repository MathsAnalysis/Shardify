package it.mathsanalysis.load.spi.cache.impl.java.impl;

import it.mathsanalysis.load.spi.cache.impl.java.entry.CacheEntry;
import it.mathsanalysis.load.spi.cache.impl.java.structure.EvictionQueue;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class RandomQueue implements EvictionQueue {

    private final ConcurrentHashMap<Object, Object> keys = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Override
    public void onPut(Object key, CacheEntry entry) {
        keys.put(key, Boolean.TRUE);
    }

    @Override
    public void onAccess(Object key, CacheEntry entry) {
        // Random doesn't care about access
    }

    @Override
    public void onRemove(Object key, CacheEntry entry) {
        keys.remove(key);
    }

    @Override
    public Object selectVictim() {
        var keyArray = keys.keySet().toArray();
        return keyArray.length > 0 ? keyArray[random.nextInt(keyArray.length)] : null;
    }

    @Override
    public void clear() {
        keys.clear();
    }
}
