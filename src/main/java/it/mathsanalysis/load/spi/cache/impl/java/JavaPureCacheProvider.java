package it.mathsanalysis.load.spi.cache.impl.java;

import it.mathsanalysis.load.spi.cache.core.CacheProvider;
import it.mathsanalysis.load.spi.cache.structure.Cache;
import it.mathsanalysis.load.spi.cache.structure.CacheConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java pure cache provider implementation
 */
public final class JavaPureCacheProvider implements CacheProvider {

    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> createCache(CacheConfiguration configuration) {
        if (closed) throw new IllegalStateException("Provider is closed");
        Objects.requireNonNull(configuration, "Configuration cannot be null");

        var cache = new JavaPureCache<K, V>(configuration.name(), configuration);
        caches.put(configuration.name(), cache);
        return cache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Optional<Cache<K, V>> getCache(String name) {
        Objects.requireNonNull(name, "Cache name cannot be null");
        return Optional.ofNullable((Cache<K, V>) caches.get(name));
    }

    @Override
    public Set<String> getCacheNames() {
        return new HashSet<>(caches.keySet());
    }

    @Override
    public void destroyCache(String name) {
        Objects.requireNonNull(name, "Cache name cannot be null");

        var cache = caches.remove(name);
        if (cache != null) {
            cache.close();
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            caches.values().forEach(Cache::close);
            caches.clear();
        }
    }

    @Override
    public String getProviderName() {
        return "JavaPure";
    }

    @Override
    public Map<String, Object> getProviderStats() {
        return Map.of(
                "provider", "JavaPure",
                "cacheCount", caches.size(),
                "closed", closed,
                "cacheNames", getCacheNames()
        );
    }
}