package it.mathsanalysis.load.spi.cache.generator;

import java.util.Map;

/**
 * Generatore di chiavi cache
 */
public interface CacheKeyGenerator<T, ID> {
    String generateKey(T item, Map<String, Object> parameters);

    String generateKeyById(ID id);
}
