package it.mathsanalysis.load.spi.cache.structure;

/**
 * Cache statistics container
 */
public record CacheStats(
    long hitCount,
    long missCount,
    long loadCount,
    long loadTime,
    long evictionCount,
    long size,
    double hitRate,
    double missRate,
    double averageLoadTime
) {
    
    public static CacheStats empty() {
        return new CacheStats(0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0);
    }
    
    public long totalCount() {
        return hitCount + missCount;
    }
}