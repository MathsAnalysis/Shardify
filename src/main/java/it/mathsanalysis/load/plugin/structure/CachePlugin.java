package it.mathsanalysis.load.plugin.structure;

import java.util.Map;

/**
 * Base class per plugin di caching
 */
public abstract class CachePlugin implements DataLoaderPlugin {

    @Override
    public <T> void afterSave(T item, T result, Map<String, Object> context) {
        updateCache(result, context);
    }

    @Override
    public <T> void afterDelete(T item, Map<String, Object> context) {
        invalidateCache(item, context);
    }

    protected abstract <T> void updateCache(T item, Map<String, Object> context);

    protected abstract <T> void invalidateCache(T item, Map<String, Object> context);
}
