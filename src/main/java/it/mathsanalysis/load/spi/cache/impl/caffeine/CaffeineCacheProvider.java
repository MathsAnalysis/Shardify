package it.mathsanalysis.load.spi.cache.impl.caffeine;

import it.mathsanalysis.load.spi.cache.core.CacheProvider;
import it.mathsanalysis.load.spi.cache.structure.Cache;
import it.mathsanalysis.load.spi.cache.structure.CacheConfiguration;

import java.util.*;

/**
 * Caffeine cache provider implementation
 */
public final class CaffeineCacheProvider implements CacheProvider {

    private final Map<String, Cache<?, ?>> caches = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean closed = false;

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> createCache(CacheConfiguration configuration) {
        if (closed) throw new IllegalStateException("Provider is closed");
        Objects.requireNonNull(configuration, "Configuration cannot be null");

        var cache = new CaffeineCache<K, V>(configuration.name(), configuration);
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
        return "Caffeine";
    }

    @Override
    public Map<String, Object> getProviderStats() {
        return Map.of(
                "provider", "Caffeine",
                "cacheCount", caches.size(),
                "closed", closed,
                "cacheNames", getCacheNames()
        );
    }
}