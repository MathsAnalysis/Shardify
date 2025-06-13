package it.mathsanalysis.load.spi.cache.wrapper;

import it.mathsanalysis.load.core.DataLoader;
import it.mathsanalysis.load.core.result.BatchResult;
import it.mathsanalysis.load.core.result.HealthStatus;
import it.mathsanalysis.load.spi.cache.core.CacheManagers;
import it.mathsanalysis.load.spi.cache.generator.CacheKeyGenerator;
import it.mathsanalysis.load.spi.cache.generator.DefaultCacheKeyGenerator;
import it.mathsanalysis.load.spi.cache.structure.Cache;
import it.mathsanalysis.load.spi.cache.structure.CacheConfiguration;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * DataLoader wrapper con cache integrata automatica
 * Fornisce caching trasparente per tutte le operazioni del DataLoader
 */
public final class CachedDataLoaderWrapper<T, ID> implements DataLoader<T, ID> {

    private final DataLoader<T, ID> delegate;
    private final Cache<String, T> cache;
    private final Cache<String, Optional<T>> notFoundCache;
    private final CacheKeyGenerator<T, ID> keyGenerator;
    private final boolean cacheNegativeResults;
    private final Duration negativeResultTtl;

    /**
     * Crea wrapper con configurazione automatica
     */
    public CachedDataLoaderWrapper(DataLoader<T, ID> delegate, String cacheName) {
        this(delegate, cacheName, CacheConfiguration.builder()
                .name(cacheName)
                .maxSize(10000)
                .defaultTtl(Duration.ofMinutes(30))
                .recordStats(true)
                .build());
    }

    /**
     * Crea wrapper con configurazione personalizzata
     */
    public CachedDataLoaderWrapper(DataLoader<T, ID> delegate, String cacheName, CacheConfiguration config) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate cannot be null");
        this.cache = CacheManagers.getDefault().getCache(cacheName, config);

        // Cache separata per risultati "not found" (evita query ripetute)
        var notFoundConfig = CacheConfiguration.builder()
                .name(cacheName + "_notfound")
                .maxSize(1000)
                .defaultTtl(Duration.ofMinutes(5))
                .recordStats(true)
                .build();

        this.notFoundCache = CacheManagers.getDefault().getCache(cacheName + "_notfound", notFoundConfig);

        this.keyGenerator = new DefaultCacheKeyGenerator<>();
        this.cacheNegativeResults = true;
        this.negativeResultTtl = Duration.ofMinutes(5);
    }

    @Override
    public T save(T item, Map<String, Object> parameters) {
        var savedItem = delegate.save(item, parameters);

        // Aggiorna cache dopo save
        var key = keyGenerator.generateKey(savedItem, parameters);
        cache.put(key, savedItem);

        // Rimuovi from notFound cache se presente
        notFoundCache.remove(key);

        return savedItem;
    }

    @Override
    public CompletableFuture<T> saveAsync(T item, Map<String, Object> parameters) {
        return delegate.saveAsync(item, parameters)
                .thenApply(savedItem -> {
                    var key = keyGenerator.generateKey(savedItem, parameters);
                    cache.putAsync(key, savedItem);
                    notFoundCache.removeAsync(key);
                    return savedItem;
                });
    }

    @Override
    public List<T> saveBatch(List<T> items, Map<String, Object> parameters) {
        var savedItems = delegate.saveBatch(items, parameters);

        // Aggiorna cache per tutti gli item
        var cacheUpdates = new HashMap<String, T>();
        var notFoundRemovals = new HashSet<String>();

        savedItems.forEach(item -> {
            var key = keyGenerator.generateKey(item, parameters);
            cacheUpdates.put(key, item);
            notFoundRemovals.add(key);
        });

        cache.putAll(cacheUpdates);
        notFoundCache.removeAll(notFoundRemovals);

        return savedItems;
    }

    @Override
    public CompletableFuture<BatchResult<T>> saveBatchAsync(Flow.Publisher<T> items, Map<String, Object> parameters) {
        return delegate.saveBatchAsync(items, parameters)
                .thenApply(result -> {
                    // Aggiorna cache async per tutti i risultati
                    result.successfulItems().forEach(item -> {
                        var key = keyGenerator.generateKey(item, parameters);
                        cache.putAsync(key, item);
                        notFoundCache.removeAsync(key);
                    });
                    return result;
                });
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) return Optional.empty();

        var key = keyGenerator.generateKeyById(id);

        // 1. Controlla cache positiva
        var cached = cache.get(key);
        if (cached.isPresent()) {
            return cached;
        }

        // 2. Controlla cache negativa
        if (cacheNegativeResults && notFoundCache.containsKey(key)) {
            return Optional.empty();
        }

        // 3. Carica dal delegate
        var result = delegate.findById(id);

        // 4. Aggiorna cache
        if (result.isPresent()) {
            cache.put(key, result.get());
        } else if (cacheNegativeResults) {
            notFoundCache.put(key, Optional.empty(), negativeResultTtl);
        }

        return result;
    }

    @Override
    public CompletableFuture<Optional<T>> findByIdAsync(ID id) {
        if (id == null) return CompletableFuture.completedFuture(Optional.empty());

        var key = keyGenerator.generateKeyById(id);

        // Controlla cache async
        return cache.getAsync(key)
                .thenCompose(cached -> {
                    if (cached.isPresent()) {
                        return CompletableFuture.completedFuture(cached);
                    }

                    // Check negative cache
                    if (cacheNegativeResults && notFoundCache.containsKey(key)) {
                        return CompletableFuture.completedFuture(Optional.<T>empty());
                    }

                    // Load from delegate
                    return delegate.findByIdAsync(id)
                            .thenApply(result -> {
                                if (result.isPresent()) {
                                    cache.putAsync(key, result.get());
                                } else if (cacheNegativeResults) {
                                    notFoundCache.putAsync(key, Optional.empty());
                                }
                                return result;
                            });
                });
    }

    @Override
    public CompletableFuture<Void> initializeStorage(Map<String, Object> parameters) {
        return delegate.initializeStorage(parameters);
    }

    @Override
    public it.mathsanalysis.load.core.result.DebugResult getDebugInfo() {
        var delegateDebug = delegate.getDebugInfo();

        var cacheInfo = Map.of(
                "cacheStats", cache.getStats(),
                "notFoundCacheStats", notFoundCache.getStats(),
                "cacheSize", cache.size(),
                "cacheName", cache.getName(),
                "cacheConfiguration", cache.getConfiguration()
        );

        var enhancedInfo = new HashMap<>(delegateDebug.additionalInfo());
        enhancedInfo.put("cache", cacheInfo);

        return new it.mathsanalysis.load.core.result.DebugResult(
                "Cached" + delegateDebug.loaderType(),
                delegateDebug.performanceStats(),
                delegateDebug.connectionStats(),
                enhancedInfo
        );
    }

    @Override
    public CompletableFuture<HealthStatus> healthCheck() {
        return delegate.healthCheck()
                .thenApply(status -> {
                    var enhancedMetrics = new HashMap<>(status.metrics());
                    enhancedMetrics.put("cacheHealth", isCacheHealthy());
                    enhancedMetrics.put("cacheStats", cache.getStats());

                    return new HealthStatus(
                            status.isHealthy() && isCacheHealthy(),
                            status.message() + (isCacheHealthy() ? " [Cache: OK]" : " [Cache: ERROR]"),
                            enhancedMetrics
                    );
                });
    }

    @Override
    public Map<String, Object> getConfiguration() {
        var delegateConfig = delegate.getConfiguration();
        var enhancedConfig = new HashMap<>(delegateConfig);
        enhancedConfig.put("cacheEnabled", true);
        enhancedConfig.put("cacheConfiguration", cache.getConfiguration());
        enhancedConfig.put("negativeResultCaching", cacheNegativeResults);
        return enhancedConfig;
    }

    @Override
    public boolean updateConfiguration(Map<String, Object> newConfig) {
        // Aggiorna configurazione del delegate
        // Cache configuration updates potrebbero essere supportate in future versioni
        return delegate.updateConfiguration(newConfig);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
        cache.close();
        notFoundCache.close();
    }

    // Cache-specific methods

    /**
     * Invalida cache entry specifica
     */
    public void evictFromCache(ID id) {
        if (id != null) {
            var key = keyGenerator.generateKeyById(id);
            cache.remove(key);
            notFoundCache.remove(key);
        }
    }

    /**
     * Invalida tutte le cache entries
     */
    public void evictAllFromCache() {
        cache.clear();
        notFoundCache.clear();
    }

    /**
     * Pre-carica item in cache
     */
    public void preloadIntoCache(T item, Map<String, Object> parameters) {
        var key = keyGenerator.generateKey(item, parameters);
        cache.put(key, item);
    }

    /**
     * Ottieni statistiche cache
     */
    public CachedDataLoaderWrapper.CacheStatistics getCacheStatistics() {
        var mainStats = cache.getStats();
        var notFoundStats = notFoundCache.getStats();

        return new CachedDataLoaderWrapper.CacheStatistics(
                mainStats.hitCount() + notFoundStats.hitCount(),
                mainStats.missCount() + notFoundStats.missCount(),
                mainStats.size(),
                notFoundStats.size(),
                mainStats.hitRate(),
                mainStats.evictionCount() + notFoundStats.evictionCount()
        );
    }

    /**
     * Ottieni cache sottostante
     */
    public Cache<String, T> getCache() {
        return cache;
    }

    /**
     * Ottieni delegate loader
     */
    public DataLoader<T, ID> getDelegate() {
        return delegate;
    }

    private boolean isCacheHealthy() {
        try {
            cache.put("health:test", null); // Test put
            cache.remove("health:test");    // Test remove
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Cache statistics container
     */
    public record CacheStatistics(
            long totalHits,
            long totalMisses,
            long mainCacheSize,
            long notFoundCacheSize,
            double hitRate,
            long totalEvictions
    ) {
        public double missRate() {
            return 1.0 - hitRate;
        }

        public long totalRequests() {
            return totalHits + totalMisses;
        }
    }
}
