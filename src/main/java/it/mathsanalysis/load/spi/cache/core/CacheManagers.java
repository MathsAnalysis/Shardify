package it.mathsanalysis.load.spi.cache.core;

/**
 * Global cache manager instance
 */
public final class CacheManagers {

    private static volatile DefaultCacheManager defaultInstance;
    private static final Object lock = new Object();

    private CacheManagers() {}

    /**
     * Get default cache manager instance (singleton)
     */
    public static DefaultCacheManager getDefault() {
        // Double-checked locking pattern
        if (defaultInstance == null) {
            synchronized (lock) {
                if (defaultInstance == null) {
                    defaultInstance = new DefaultCacheManager();
                }
            }
        }
        return defaultInstance;
    }

    /**
     * Set custom default cache manager
     */
    public static void setDefault(DefaultCacheManager manager) {
        synchronized (lock) {
            if (defaultInstance != null) {
                defaultInstance.close();
            }
            defaultInstance = manager;
        }
    }

    /**
     * Create new cache manager with specified provider
     */
    public static DefaultCacheManager create(CacheProvider provider) {
        return new DefaultCacheManager(provider);
    }

    /**
     * Shutdown default cache manager
     */
    public static void shutdownDefault() {
        synchronized (lock) {
            if (defaultInstance != null) {
                defaultInstance.close();
                defaultInstance = null;
            }
        }
    }
}