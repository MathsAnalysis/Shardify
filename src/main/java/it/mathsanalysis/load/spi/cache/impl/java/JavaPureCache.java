package it.mathsanalysis.load.spi.cache.impl.java;

import it.mathsanalysis.load.spi.cache.event.CacheEventListener;
import it.mathsanalysis.load.spi.cache.impl.java.entry.CacheEntry;
import it.mathsanalysis.load.spi.cache.impl.java.impl.*;
import it.mathsanalysis.load.spi.cache.impl.java.structure.EvictionQueue;
import it.mathsanalysis.load.spi.cache.structure.Cache;
import it.mathsanalysis.load.spi.cache.structure.CacheConfiguration;
import it.mathsanalysis.load.spi.cache.structure.CacheStats;
import it.mathsanalysis.load.spi.cache.type.EvictionPolicy;
import it.mathsanalysis.load.spi.cache.type.RemovalCause;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Pure Java cache implementation without external dependencies
 * High-performance cache with configurable eviction policies
 */
public final class JavaPureCache<K, V> implements Cache<K, V> {

    private String name;
    private final CacheConfiguration configuration;
    private final ConcurrentHashMap<K, CacheEntry<V>> storage = new ConcurrentHashMap<>();
    private final EvictionQueue evictionQueue;
    private final List<CacheEventListener<K, V>> listeners = new CopyOnWriteArrayList<>();

    // Statistics
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();
    private final AtomicLong loadCount = new AtomicLong();
    private final AtomicLong loadTime = new AtomicLong();

    private volatile boolean closed = false;

    // Scheduled executor for periodic cleanup of expired entries
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Cache-Cleanup-" + name);
        thread.setDaemon(true); // Important for Minecraft
        return thread;
    });

    public JavaPureCache(String name, CacheConfiguration configuration) {
        this.name = Objects.requireNonNull(name, "Cache name cannot be null");
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        this.evictionQueue = createEvictionQueue(configuration.evictionPolicy());

        // Schedule automatic cleanup every 30 seconds
        cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupExpired,
                30, 30,
                TimeUnit.SECONDS
        );
    }

    @Override
    public Optional<V> get(K key) {
        if (closed) throw new IllegalStateException("Cache is closed");
        Objects.requireNonNull(key, "Key cannot be null");

        cleanupExpired();

        CacheEntry entry = storage.get(key);
        if (entry != null && !entry.isExpired()) {
            entry.updateAccessTime();
            evictionQueue.onAccess(key, entry);
            hitCount.incrementAndGet();
            notifyGet(key, (V) entry.value, true);
            return (Optional<V>) Optional.of(entry.value);
        } else {
            if (entry != null && entry.isExpired()) {
                storage.remove(key);
                evictionQueue.onRemove(key, entry);
                notifyRemove(key, (V) entry.value, RemovalCause.EXPIRED);
            }
            missCount.incrementAndGet();
            notifyGet(key, null, false);
            return Optional.empty();
        }
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(loader, "Loader cannot be null");

        var existing = get(key);
        if (existing.isPresent()) {
            return existing.get();
        }

        var startTime = System.nanoTime();
        var value = loader.apply(key);
        loadTime.addAndGet(System.nanoTime() - startTime);
        loadCount.incrementAndGet();

        if (value != null) {
            put(key, value);
        }

        return value;
    }

    @Override
    public void put(K key, V value) {
        if (closed) throw new IllegalStateException("Cache is closed");
        Objects.requireNonNull(key, "Key cannot be null");
        validateValue(value);

        put(key, value, configuration.defaultTtl());
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        if (closed) throw new IllegalStateException("Cache is closed");
        Objects.requireNonNull(key, "Key cannot be null");
        validateValue(value);

        cleanupExpired();

        // Check if we need to evict
        if (storage.size() >= configuration.maxSize() && !storage.containsKey(key)) {
            evictLeastValuable();
        }

        var entry = new CacheEntry<>(value, ttl);
        var oldEntry = storage.put(key, entry);

        if (oldEntry != null) {
            evictionQueue.onRemove(key, oldEntry);
        }

        evictionQueue.onPut(key, entry);
        notifyPut(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if (closed) throw new IllegalStateException("Cache is closed");
        Objects.requireNonNull(key, "Key cannot be null");
        validateValue(value);

        var existing = get(key);
        if (existing.isPresent()) {
            return existing.get();
        }

        put(key, value);
        return null;
    }

    @Override
    public Optional<V> remove(K key) {
        if (closed) throw new IllegalStateException("Cache is closed");
        Objects.requireNonNull(key, "Key cannot be null");

        var entry = storage.remove(key);
        if (entry != null) {
            evictionQueue.onRemove(key, entry);
            notifyRemove(key, entry.value, RemovalCause.EXPLICIT);
            return Optional.of(entry.value);
        }
        return Optional.empty();
    }

    @Override
    public boolean containsKey(K key) {
        if (closed) throw new IllegalStateException("Cache is closed");
        Objects.requireNonNull(key, "Key cannot be null");

        var entry = storage.get(key);
        if (entry != null && entry.isExpired()) {
            storage.remove(key);
            evictionQueue.onRemove(key, entry);
            return false;
        }
        return entry != null;
    }

    @Override
    public Map<K, V> asMap() {
        if (closed) throw new IllegalStateException("Cache is closed");

        cleanupExpired();
        var result = new HashMap<K, V>();
        storage.forEach((key, entry) -> {
            if (!entry.isExpired()) {
                result.put(key, entry.value);
            }
        });
        return result;
    }

    @Override
    public void clear() {
        if (closed) throw new IllegalStateException("Cache is closed");

        storage.clear();
        evictionQueue.clear();
        notifyClear();
    }

    @Override
    public long size() {
        if (closed) return 0;

        cleanupExpired();
        return storage.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Map<K, V> getAll(Set<K> keys) {
        Objects.requireNonNull(keys, "Keys cannot be null");

        var result = new HashMap<K, V>();
        keys.forEach(key -> {
            var value = get(key);
            value.ifPresent(v -> result.put(key, v));
        });
        return result;
    }

    @Override
    public void putAll(Map<K, V> map) {
        Objects.requireNonNull(map, "Map cannot be null");

        map.forEach(this::put);
    }

    @Override
    public void removeAll(Set<K> keys) {
        Objects.requireNonNull(keys, "Keys cannot be null");

        keys.forEach(this::remove);
    }

    @Override
    public CompletableFuture<Optional<V>> getAsync(K key) {
        return CompletableFuture.supplyAsync(() -> get(key));
    }

    @Override
    public CompletableFuture<Void> putAsync(K key, V value) {
        return CompletableFuture.runAsync(() -> put(key, value));
    }

    @Override
    public CompletableFuture<Optional<V>> removeAsync(K key) {
        return CompletableFuture.supplyAsync(() -> remove(key));
    }

    @Override
    public CacheStats getStats() {
        if (closed) return CacheStats.empty();

        var totalRequests = hitCount.get() + missCount.get();
        var hitRate = totalRequests > 0 ? (double) hitCount.get() / totalRequests : 0.0;
        var missRate = 1.0 - hitRate;
        var avgLoadTime = loadCount.get() > 0 ? (double) loadTime.get() / loadCount.get() : 0.0;

        return new CacheStats(
                hitCount.get(),
                missCount.get(),
                loadCount.get(),
                loadTime.get(),
                evictionCount.get(),
                storage.size(),
                hitRate,
                missRate,
                avgLoadTime
        );
    }

    @Override
    public void resetStats() {
        hitCount.set(0);
        missCount.set(0);
        loadCount.set(0);
        loadTime.set(0);
        evictionCount.set(0);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CacheConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean updateConfiguration(CacheConfiguration newConfig) {
        // Java Pure cache doesn't support runtime configuration updates
        return false;
    }

    @Override
    public void cleanUp() {
        if (!closed) {
            cleanupExpired();
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;

            // Shutdown cleanup executor
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            storage.clear();
            evictionQueue.clear();
            listeners.clear();
        }
    }

    @Override
    public void evict(K key) {
        if (!closed) {
            remove(key);
        }
    }

    @Override
    public void evictAll(java.util.function.Predicate<K> predicate) {
        if (closed) return;
        Objects.requireNonNull(predicate, "Predicate cannot be null");

        var keysToEvict = storage.keySet().stream()
                .filter(predicate)
                .collect(java.util.stream.Collectors.toSet());

        removeAll(keysToEvict);
    }

    @Override
    public long estimatedSize() {
        return size();
    }

    @Override
    public void addListener(CacheEventListener<K, V> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        listeners.add(listener);
    }

    @Override
    public void removeListener(CacheEventListener<K, V> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        listeners.remove(listener);
    }

    // Private helper methods

    private void validateValue(V value) {
        if (value == null && !configuration.allowNullValues()) {
            throw new IllegalArgumentException("Null values not allowed");
        }
    }

    private void cleanupExpired() {
        var now = Instant.now();
        var expiredKeys = new ArrayList<K>();

        storage.forEach((key, entry) -> {
            if (entry.isExpired(now)) {
                expiredKeys.add(key);
            }
        });

        expiredKeys.forEach(key -> {
            var entry = storage.remove(key);
            if (entry != null) {
                evictionQueue.onRemove(key, entry);
                notifyRemove(key, entry.value, RemovalCause.EXPIRED);
            }
        });
    }

    private void evictLeastValuable() {
        var victim = evictionQueue.selectVictim();
        if (victim != null) {
            var entry = storage.remove(victim);
            if (entry != null) {
                evictionQueue.onRemove(victim, entry);
                evictionCount.incrementAndGet();
                notifyEvict((K) victim, entry.value, RemovalCause.SIZE);
            }
        }
    }

    private EvictionQueue createEvictionQueue(EvictionPolicy policy) {
        return switch (policy) {
            case LRU -> new LRUQueue();
            case LFU -> new LFUQueue();
            case FIFO -> new FIFOQueue();
            case RANDOM -> new RandomQueue();
            case NONE -> new NoEvictionQueue();
        };
    }

    private void notifyPut(K key, V value) {
        listeners.forEach(listener -> {
            try {
                listener.onPut(key, value);
            } catch (Exception e) {
                System.err.println("Cache listener error on put: " + e.getMessage());
            }
        });
    }

    private void notifyGet(K key, V value, boolean isHit) {
        listeners.forEach(listener -> {
            try {
                listener.onGet(key, value, isHit);
            } catch (Exception e) {
                System.err.println("Cache listener error on get: " + e.getMessage());
            }
        });
    }

    private void notifyRemove(K key, V value, RemovalCause cause) {
        listeners.forEach(listener -> {
            try {
                listener.onRemove(key, value, cause);
            } catch (Exception e) {
                System.err.println("Cache listener error on remove: " + e.getMessage());
            }
        });
    }

    private void notifyEvict(K key, V value, RemovalCause cause) {
        listeners.forEach(listener -> {
            try {
                listener.onEvict(key, value, cause);
            } catch (Exception e) {
                System.err.println("Cache listener error on evict: " + e.getMessage());
            }
        });
    }

    private void notifyClear() {
        listeners.forEach(listener -> {
            try {
                listener.onClear();
            } catch (Exception e) {
                System.err.println("Cache listener error on clear: " + e.getMessage());
            }
        });
    }

}