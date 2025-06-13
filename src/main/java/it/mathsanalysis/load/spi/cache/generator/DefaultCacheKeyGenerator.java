package it.mathsanalysis.load.spi.cache.generator;

import java.util.*;

/**
 * Implementazione default del generatore di chiavi
 */
public class DefaultCacheKeyGenerator<T, ID> implements CacheKeyGenerator<T, ID> {
    
    @Override
    public String generateKey(T item, Map<String, Object> parameters) {
        if (item == null) return "null";
        
        var keyBuilder = new StringBuilder();
        keyBuilder.append(item.getClass().getSimpleName()).append(":");
        
        // Usa hashCode come fallback
        keyBuilder.append(item.hashCode());
        
        // Aggiungi parametri se rilevanti per la cache
        if (parameters != null && !parameters.isEmpty()) {
            keyBuilder.append(":params:").append(parameters.hashCode());
        }
        
        return keyBuilder.toString();
    }
    
    @Override
    public String generateKeyById(ID id) {
        return "id:" + (id != null ? id.toString() : "null");
    }
}
