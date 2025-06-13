package it.mathsanalysis.load.spi.cache.factory;

import it.mathsanalysis.load.core.DataLoader;
import it.mathsanalysis.load.spi.cache.structure.CacheConfiguration;
import it.mathsanalysis.load.spi.cache.wrapper.CachedDataLoaderWrapper;

import java.time.Duration;

/**
 * Factory per creare DataLoader cached
 */
public final class CachedDataLoaderFactory {

    private CachedDataLoaderFactory() {
    }

    /**
     * Wrappa DataLoader con cache automatica
     */
    public static <T, ID> CachedDataLoaderWrapper<T, ID> wrap(DataLoader<T, ID> loader, String cacheName) {
        return new CachedDataLoaderWrapper<>(loader, cacheName);
    }

    /**
     * Wrappa DataLoader con cache configurata
     */
    public static <T, ID> CachedDataLoaderWrapper<T, ID> wrap(
            DataLoader<T, ID> loader,
            String cacheName,
            CacheConfiguration config) {
        return new CachedDataLoaderWrapper<>(loader, cacheName, config);
    }

    /**
     * Wrappa DataLoader con cache ottimizzata per letture frequenti
     */
    public static <T, ID> CachedDataLoaderWrapper<T, ID> wrapForReads(DataLoader<T, ID> loader, String cacheName) {
        var config = CacheConfiguration.builder()
                .name(cacheName)
                .maxSize(50000)
                .defaultTtl(Duration.ofHours(1))
                .expireAfterAccess(Duration.ofMinutes(30))
                .recordStats(true)
                .build();

        return new CachedDataLoaderWrapper<>(loader, cacheName, config);
    }

    /**
     * Wrappa DataLoader con cache ottimizzata per scritture frequenti
     */
    public static <T, ID> CachedDataLoaderWrapper<T, ID> wrapForWrites(DataLoader<T, ID> loader, String cacheName) {
        var config = CacheConfiguration.builder()
                .name(cacheName)
                .maxSize(10000)
                .defaultTtl(Duration.ofMinutes(15))
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats(true)
                .build();

        return new CachedDataLoaderWrapper<>(loader, cacheName, config);
    }

    /**
     * Controlla se loader ha bisogno di caching
     */
    public static boolean needsCaching(Class<?> itemType) {
        // Logica per determinare se un tipo necessita di caching
        return !itemType.isPrimitive() &&
                !itemType.equals(String.class) &&
                !Number.class.isAssignableFrom(itemType);
    }
}
