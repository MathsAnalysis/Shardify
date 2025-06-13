package it.mathsanalysis.load.spi.cache.core;

import it.mathsanalysis.load.spi.cache.impl.caffeine.CaffeineCacheProvider;
import it.mathsanalysis.load.spi.cache.impl.java.JavaPureCacheProvider;
import it.mathsanalysis.load.spi.cache.structure.Cache;
import it.mathsanalysis.load.spi.cache.structure.CacheConfiguration;
import it.mathsanalysis.load.spi.cache.structure.CacheStats;
import it.mathsanalysis.load.spi.cache.type.EvictionPolicy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central cache manager for managing multiple cache providers and instances
 * Thread-safe with provider auto-detection and advanced management features
 */
public final class DefaultCacheManager implements CacheManager {
    
    private final Map<String, CacheProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private final CacheProvider defaultProvider;
    private volatile CacheConfiguration globalConfiguration;
    private volatile boolean closed = false;
    
    /**
     * Create cache manager with auto-detected providers
     */
    public DefaultCacheManager() {
        this(detectBestProvider());
    }
    
    /**
     * Create cache manager with specific default provider
     */
    public DefaultCacheManager(CacheProvider defaultProvider) {
        this.defaultProvider = Objects.requireNonNull(defaultProvider, "Default provider cannot be null");
        this.globalConfiguration = CacheConfiguration.builder().build();
        
        // Register available providers
        registerProviders();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name, CacheConfiguration configuration) {
        if (closed) throw new IllegalStateException("Cache manager is closed");
        Objects.requireNonNull(name, "Cache name cannot be null");
        Objects.requireNonNull(configuration, "Configuration cannot be null");
        
        return (Cache<K, V>) caches.computeIfAbsent(name, cacheName -> {
            var config = mergeWithGlobalConfig(configuration);
            return defaultProvider.createCache(config);
        });
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return getCache(name, globalConfiguration);
    }
    
    /**
     * Get cache with specific provider
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name, String providerName, CacheConfiguration configuration) {
        if (closed) throw new IllegalStateException("Cache manager is closed");
        Objects.requireNonNull(name, "Cache name cannot be null");
        Objects.requireNonNull(providerName, "Provider name cannot be null");
        Objects.requireNonNull(configuration, "Configuration cannot be null");
        
        var provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provider not found: " + providerName);
        }
        
        var fullName = providerName + ":" + name;
        return (Cache<K, V>) caches.computeIfAbsent(fullName, cacheName -> {
            var config = mergeWithGlobalConfig(configuration);
            return provider.createCache(config);
        });
    }
    
    @Override
    public Map<String, Cache<?, ?>> getAllCaches() {
        return Map.copyOf(caches);
    }
    
    @Override
    public void removeCache(String name) {
        Objects.requireNonNull(name, "Cache name cannot be null");
        
        var cache = caches.remove(name);
        if (cache != null) {
            cache.close();
        }
    }
    
    @Override
    public void clearAll() {
        caches.values().forEach(Cache::clear);
    }
    
    @Override
    public Map<String, CacheStats> getAllStats() {
        var stats = new HashMap<String, CacheStats>();
        caches.forEach((name, cache) -> stats.put(name, cache.getStats()));
        return stats;
    }
    
    @Override
    public void setGlobalConfiguration(CacheConfiguration configuration) {
        this.globalConfiguration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    }
    
    @Override
    public CacheConfiguration getGlobalConfiguration() {
        return globalConfiguration;
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            caches.values().forEach(Cache::close);
            caches.clear();
            providers.values().forEach(CacheProvider::close);
            providers.clear();
        }
    }
    
    // Additional management methods
    
    /**
     * Register new cache provider
     */
    public void registerProvider(String name, CacheProvider provider) {
        Objects.requireNonNull(name, "Provider name cannot be null");
        Objects.requireNonNull(provider, "Provider cannot be null");
        
        if (closed) throw new IllegalStateException("Cache manager is closed");
        
        providers.put(name, provider);
    }
    
    /**
     * Get all registered providers
     */
    public Map<String, CacheProvider> getProviders() {
        return Map.copyOf(providers);
    }
    
    /**
     * Get provider by name
     */
    public Optional<CacheProvider> getProvider(String name) {
        Objects.requireNonNull(name, "Provider name cannot be null");
        return Optional.ofNullable(providers.get(name));
    }
    
    /**
     * Get cache statistics summary
     */
    public CacheManagerStats getManagerStats() {
        var totalCaches = caches.size();
        var totalHits = 0L;
        var totalMisses = 0L;
        var totalSize = 0L;
        
        for (var cache : caches.values()) {
            var stats = cache.getStats();
            totalHits += stats.hitCount();
            totalMisses += stats.missCount();
            totalSize += stats.size();
        }
        
        var totalRequests = totalHits + totalMisses;
        var overallHitRate = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
        
        return new CacheManagerStats(
            totalCaches,
            providers.size(),
            totalHits,
            totalMisses,
            totalSize,
            overallHitRate,
            closed
        );
    }
    
    /**
     * Cleanup all caches
     */
    public void cleanupAll() {
        caches.values().forEach(Cache::cleanUp);
    }
    
    /**
     * Reset all cache statistics
     */
    public void resetAllStats() {
        caches.values().forEach(Cache::resetStats);
    }
    
    /**
     * Get detailed cache information
     */
    public Map<String, CacheInfo> getCacheInfo() {
        var info = new HashMap<String, CacheInfo>();
        
        caches.forEach((name, cache) -> {
            var stats = cache.getStats();
            var config = cache.getConfiguration();
            
            info.put(name, new CacheInfo(
                name,
                cache.getClass().getSimpleName(),
                stats,
                config,
                cache.estimatedSize()
            ));
        });
        
        return info;
    }
    
    /**
     * Create cache builder for fluent configuration
     */
    public CacheBuilder cacheBuilder(String name) {
        return new CacheBuilder(this, name);
    }


    private void registerProviders() {
        // Register Java Pure provider (always available)
        providers.put("JavaPure", new JavaPureCacheProvider());
        
        // Register Caffeine provider if available
        try {
            Class.forName("com.github.benmanes.caffeine.cache.Caffeine");
            providers.put("Caffeine", new CaffeineCacheProvider());
        } catch (ClassNotFoundException e) {
            // Caffeine not available, skip
        }
    }
    
    private static CacheProvider detectBestProvider() {
        // Try Caffeine first (best performance)
        try {
            Class.forName("com.github.benmanes.caffeine.cache.Caffeine");
            return new CaffeineCacheProvider();
        } catch (ClassNotFoundException e) {
            // Fall back to Java Pure
            return new JavaPureCacheProvider();
        }
    }
    
    private CacheConfiguration mergeWithGlobalConfig(CacheConfiguration specific) {
        if (specific.name().equals("default")) {
            return CacheConfiguration.builder()
                .name(specific.name())
                .maxSize(specific.maxSize() > 0 ? specific.maxSize() : globalConfiguration.maxSize())
                .defaultTtl(specific.defaultTtl() != null ? specific.defaultTtl() : globalConfiguration.defaultTtl())
                .maxIdleDuration(specific.maxIdleDuration() != null ? specific.maxIdleDuration() : globalConfiguration.maxIdleDuration())
                .recordStats(specific.recordStats() || globalConfiguration.recordStats())
                .allowNullValues(specific.allowNullValues() || globalConfiguration.allowNullValues())
                .evictionPolicy(specific.evictionPolicy() != EvictionPolicy.LRU ? specific.evictionPolicy() : globalConfiguration.evictionPolicy())
                .concurrencyLevel(specific.concurrencyLevel() > 0 ? specific.concurrencyLevel() : globalConfiguration.concurrencyLevel())
                .build();
        }
        return specific;
    }
    

    /**
     * Cache manager statistics
     */
    public record CacheManagerStats(
        int totalCaches,
        int totalProviders,
        long totalHits,
        long totalMisses,
        long totalSize,
        double overallHitRate,
        boolean closed
    ) {}
    
    /**
     * Cache information container
     */
    public record CacheInfo(
        String name,
        String implementation,
        CacheStats stats,
        CacheConfiguration configuration,
        long estimatedSize
    ) {}
    
    /**
     * Fluent cache builder
     */
    public static final class CacheBuilder {
        private final DefaultCacheManager manager;
        private final String name;
        private final CacheConfiguration.Builder configBuilder = CacheConfiguration.builder();
        private String providerName;
        
        CacheBuilder(DefaultCacheManager manager, String name) {
            this.manager = manager;
            this.name = name;
        }
        
        public CacheBuilder maxSize(long maxSize) {
            configBuilder.maxSize(maxSize);
            return this;
        }
        
        public CacheBuilder defaultTtl(java.time.Duration ttl) {
            configBuilder.defaultTtl(ttl);
            return this;
        }
        
        public CacheBuilder evictionPolicy(EvictionPolicy policy) {
            configBuilder.evictionPolicy(policy);
            return this;
        }
        
        public CacheBuilder recordStats(boolean recordStats) {
            configBuilder.recordStats(recordStats);
            return this;
        }
        
        public CacheBuilder allowNullValues(boolean allowNullValues) {
            configBuilder.allowNullValues(allowNullValues);
            return this;
        }
        
        public CacheBuilder provider(String providerName) {
            this.providerName = providerName;
            return this;
        }
        
        public CacheBuilder expireAfterWrite(java.time.Duration duration) {
            configBuilder.expireAfterWrite(duration);
            return this;
        }
        
        public CacheBuilder expireAfterAccess(java.time.Duration duration) {
            configBuilder.expireAfterAccess(duration);
            return this;
        }
        
        public <K, V> Cache<K, V> build() {
            var config = configBuilder.name(name).build();
            
            if (providerName != null) {
                return manager.getCache(name, providerName, config);
            } else {
                return manager.getCache(name, config);
            }
        }
    }
}

