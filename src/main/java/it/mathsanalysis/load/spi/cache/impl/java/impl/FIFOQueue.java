package it.mathsanalysis.load.spi.cache.impl.java.impl;

import it.mathsanalysis.load.spi.cache.impl.java.entry.CacheEntry;
import it.mathsanalysis.load.spi.cache.impl.java.structure.EvictionQueue;

import java.util.concurrent.ConcurrentLinkedQueue;

public class FIFOQueue implements EvictionQueue {

    private final ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue<>();

    @Override
    public void onPut(Object key, CacheEntry entry) {
        queue.offer(key);
    }

    @Override
    public void onAccess(Object key, CacheEntry entry) {
        // FIFO doesn't care about access
    }

    @Override
    public void onRemove(Object key, CacheEntry entry) {
        queue.remove(key);
    }

    @Override
    public Object selectVictim() {
        return queue.poll();
    }

    @Override
    public void clear() {
        queue.clear();
    }
}
