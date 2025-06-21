package it.mathsanalysis.load.core;

import it.mathsanalysis.load.core.result.BatchResult;
import it.mathsanalysis.load.core.result.DebugResult;
import it.mathsanalysis.load.core.result.HealthStatus;
import it.mathsanalysis.load.metrics.PerformanceMetrics;
import it.mathsanalysis.load.resilience.event.structure.DataLoaderEventListener;
import it.mathsanalysis.load.resilience.exception.DataLoaderException;
import it.mathsanalysis.load.util.StreamCollector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base implementation of DataLoader using Template Method pattern.
 *
 * Provides common functionality and algorithms while allowing subclasses
 * to customize specific database operations. This approach ensures consistency
 * across different database implementations while maintaining flexibility.
 * <p>
 */
public abstract class AbstractDataLoader<T, ID> implements DataLoader<T, ID> {

    // Core configuration
    protected final Class<T> itemType;
    protected final Class<ID> idType;
    protected final PerformanceMetrics metrics;
    protected final Map<String, Object> configuration;

    // Plugin and event system
    protected final List<DataLoaderEventListener> eventListeners;

    // State management
    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final AtomicLong operationCounter = new AtomicLong(0);

    // Configuration constants
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final long DEFAULT_TIMEOUT_MS = 30000;
    private static final boolean DEFAULT_ENABLE_METRICS = true;
    private static final boolean DEFAULT_ENABLE_PLUGINS = true;
    private static final boolean DEFAULT_ENABLE_EVENTS = true;

    /**
     * Constructor for abstract data loader
     *
     * @param itemType Class of items this loader handles
     * @param idType Class of item identifiers
     * @param configuration Initial configuration map
     */
    protected AbstractDataLoader(Class<T> itemType, Class<ID> idType, Map<String, Object> configuration) {
        this.itemType = Objects.requireNonNull(itemType, "Item type cannot be null");
        this.idType = Objects.requireNonNull(idType, "ID type cannot be null");
        this.configuration = new ConcurrentHashMap<>(Objects.requireNonNull(configuration, "Configuration cannot be null"));
        this.metrics = new PerformanceMetrics();
        this.eventListeners = new ArrayList<>();
    }

    /**
     * Constructor with plugin and event support
     *
     * @param itemType Class of items this loader handles
     * @param idType Class of item identifiers
     * @param configuration Initial configuration map
     * @param eventListeners List of event listeners to register
     */
    protected AbstractDataLoader(Class<T> itemType, Class<ID> idType, Map<String, Object> configuration, List<DataLoaderEventListener> eventListeners) {
        this(itemType, idType, configuration);
        if (eventListeners != null) {
            this.eventListeners.addAll(eventListeners);
        }
    }


    @Override
    public final T save(T item, Map<String, Object> parameters) {
        // Template method implementation with common algorithm

        validateSaveInput(item, parameters);

        var startTime = System.nanoTime();

        try {
            var savedItem = doSave(item, parameters);

            if (isMetricsEnabled()) {
                metrics.recordOperation("save", System.nanoTime() - startTime);
            }

            return savedItem;

        } catch (Exception e) {
            if (isMetricsEnabled()) {
                metrics.recordOperation("save_error", System.nanoTime() - startTime);
            }
            throw wrapException("Save operation failed", "SAVE_ERROR", e, DataLoaderException.ErrorSeverity.CRITICAL);
        }
    }

    @Override
    public final CompletableFuture<T> saveAsync(T item, Map<String, Object> parameters) {
        // Validate synchronously to fail fast
        validateSaveInput(item, parameters);

        return CompletableFuture.supplyAsync(() -> save(item, parameters)).whenComplete((result, throwable) -> {
            if (throwable != null && isMetricsEnabled()) {
                metrics.recordOperation("save_async_error", 0);
            }
        });
    }


    @Override
    public final List<T> saveBatch(List<T> items, Map<String, Object> parameters) {
        // Template method for batch operations

        // Step 1: Validate batch input
        validateBatchInput(items, parameters);

        // Early return for empty batches
        if (items.isEmpty()) {
            return List.of();
        }

        // Step 2: Record batch operation start
        var startTime = System.nanoTime();

        try {
            // Step 3: Execute database-specific batch save
            var savedItems = doSaveBatch(items, parameters);

            // Step 4: Record batch success metrics
            if (isMetricsEnabled()) {
                metrics.recordBatchOperation("saveBatch", items.size(), System.nanoTime() - startTime);
            }

            return savedItems;

        } catch (Exception e) {
            // Step 5: Handle batch errors
            if (isMetricsEnabled()) {
                metrics.recordOperation("saveBatch_error", System.nanoTime() - startTime);
            }
            throw wrapException("Batch save operation failed", "BATCH_SAVE_ERROR", e, DataLoaderException.ErrorSeverity.CRITICAL);
        }
    }

    @Override
    public final CompletableFuture<BatchResult<T>> saveBatchAsync(Flow.Publisher<T> items, Map<String, Object> parameters) {

        Objects.requireNonNull(items, "Items publisher cannot be null");
        Map<String, Object> safeParameters = parameters != null ? parameters : Map.of();

        return CompletableFuture.supplyAsync(() -> {
            // Use StreamCollector to gather items from reactive stream
            var collector = createStreamCollector(safeParameters);
            items.subscribe(collector);

            // Get collected items (blocks until completion or timeout)
            var collectedItems = collector.getItems();

            // Perform batch save on collected items
            var savedItems = saveBatch(collectedItems, safeParameters);

            // Return comprehensive batch result
            return new BatchResult<>(
                    savedItems,
                    collectedItems.size(),
                    collector.getErrors()
            );
        }).exceptionally(throwable -> {
            // Handle async batch errors
            if (isMetricsEnabled()) {
                metrics.recordOperation("saveBatchAsync_error", 0);
            }
            throw wrapException("Async batch save failed", "ASYNC_BATCH_ERROR", throwable, DataLoaderException.ErrorSeverity.CRITICAL);
        });
    }


    @Override
    public final Optional<T> findById(ID id) {
        // Template method for find operations

        // Step 1: Handle null ID gracefully
        if (id == null) {
            return Optional.empty();
        }

        // Step 2: Record operation start
        var startTime = System.nanoTime();

        try {
            // Step 3: Execute database-specific find
            var result = doFindById(id);

            // Step 4: Record success metrics
            if (isMetricsEnabled()) {
                var operationType = result.isPresent() ? "findById_found" : "findById_notfound";
                metrics.recordOperation(operationType, System.nanoTime() - startTime);
            }

            return result;

        } catch (Exception e) {
            // Step 5: Handle find errors
            if (isMetricsEnabled()) {
                metrics.recordOperation("findById_error", System.nanoTime() - startTime);
            }
            throw wrapException("Find by ID operation failed", "FIND_ERROR", e, DataLoaderException.ErrorSeverity.CRITICAL);
        }
    }

    @Override
    public final CompletableFuture<Optional<T>> findByIdAsync(ID id) {
        return CompletableFuture.supplyAsync(() -> findById(id)).whenComplete((result, throwable) -> {
            if (throwable != null && isMetricsEnabled()) {
                metrics.recordOperation("findByIdAsync_error", 0);
            }
        });
    }

    @Override
    public final CompletableFuture<Void> initializeStorage(Map<String, Object> parameters) {
        Map<String, Object> safeParameters = parameters != null ? parameters : Map.of();

        return CompletableFuture.runAsync(() -> {
            var startTime = System.nanoTime();

            try {
                doInitializeStorage(safeParameters);

                if (isMetricsEnabled()) {
                    metrics.recordOperation("initializeStorage", System.nanoTime() - startTime);
                }

            } catch (Exception e) {
                if (isMetricsEnabled()) {
                    metrics.recordOperation("initializeStorage_error", System.nanoTime() - startTime);
                }
                throw wrapException("Storage initialization failed", "INIT_ERROR", e, DataLoaderException.ErrorSeverity.CRITICAL);
            }
        });
    }

    @Override
    public final DebugResult getDebugInfo() {
        var loaderInfo = new HashMap<String, Object>();
        loaderInfo.put("itemType", itemType.getSimpleName());
        loaderInfo.put("idType", idType.getSimpleName());
        loaderInfo.put("implementation", this.getClass().getSimpleName());
        loaderInfo.put("initialized", initialized.get());
        loaderInfo.put("operationCount", operationCounter.get());
        loaderInfo.put("eventListenerCount", eventListeners.size());
        loaderInfo.put("configuration", getFilteredConfiguration());

        return new DebugResult(
                this.getClass().getSimpleName(),
                metrics.getStats(),
                getConnectionStats(),
                loaderInfo
        );
    }

    @Override
    public final CompletableFuture<HealthStatus> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var baseHealth = doHealthCheck();

                // Add framework-level health information
                var enhancedMetrics = new HashMap<>(baseHealth.metrics());
                enhancedMetrics.put("initialized", initialized.get());
                enhancedMetrics.put("operationCount", operationCounter.get());
                enhancedMetrics.put("eventListenerCount", eventListeners.size());

                return new HealthStatus(
                        baseHealth.isHealthy(),
                        baseHealth.message(),
                        enhancedMetrics
                );

            } catch (Exception e) {
                Map<String, Object> errorMetrics = Map.of(
                        "error", e.getMessage(),
                        "errorType", e.getClass().getSimpleName(),
                        "timestamp", System.currentTimeMillis(),
                        "initialized", initialized.get()
                );
                return HealthStatus.unhealthy("Health check failed: " + e.getMessage(), errorMetrics);
            }
        });
    }

    /**
     * Register an event listener with this data loader
     */
    public final void addEventListener(DataLoaderEventListener listener) {
        Objects.requireNonNull(listener, "Event listener cannot be null");
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    /**
     * Unregister an event listener from this data loader
     */
    public final void removeEventListener(DataLoaderEventListener listener) {
        eventListeners.remove(listener);
    }


    /**
     * Database-specific save implementation
     *
     * @param item The item to save
     * @param parameters Operation parameters
     * @return The saved item with any generated fields
     */
    protected abstract T doSave(T item, Map<String, Object> parameters);

    /**
     * Database-specific batch save implementation
     *
     * @param items List of items to save
     * @param parameters Batch operation parameters
     * @return List of saved items in same order as input
     */
    protected abstract List<T> doSaveBatch(List<T> items, Map<String, Object> parameters);

    /**
     * Database-specific find by ID implementation
     *
     * @param id The item identifier
     * @return Optional containing the item if found
     */
    protected abstract Optional<T> doFindById(ID id);

    /**
     * Database-specific storage initialization
     *
     * @param parameters Initialization parameters
     */
    protected abstract void doInitializeStorage(Map<String, Object> parameters);

    /**
     * Database-specific health check implementation
     *
     * @return Health status of the database connection
     */
    protected abstract HealthStatus doHealthCheck();

    /**
     * Get database connection statistics
     *
     * @return Map of connection statistics
     */
    protected abstract Map<String, Object> getConnectionStats();


    /**
     * Get current configuration
     */
    public final Map<String, Object> getConfiguration() {
        return Map.copyOf(configuration);
    }

    /**
     * Update configuration (thread-safe)
     */
    protected final void updateConfiguration(String key, Object value) {
        configuration.put(key, value);
    }

    /**
     * Get configuration value with default
     */
    protected final <V> V getConfigurationValue(String key, V defaultValue) {
        @SuppressWarnings("unchecked")
        V value = (V) configuration.get(key);
        return value != null ? value : defaultValue;
    }

    protected final boolean isMetricsEnabled() {
        return getConfigurationValue("enableMetrics", DEFAULT_ENABLE_METRICS);
    }

    protected final boolean isPluginsEnabled() {
        return getConfigurationValue("enablePlugins", DEFAULT_ENABLE_PLUGINS);
    }

    protected final boolean isEventsEnabled() {
        return getConfigurationValue("enableEvents", DEFAULT_ENABLE_EVENTS);
    }

    protected final int getBatchSize() {
        return getConfigurationValue("batchSize", DEFAULT_BATCH_SIZE);
    }

    protected final long getTimeoutMs() {
        return getConfigurationValue("timeoutMs", DEFAULT_TIMEOUT_MS);
    }

    private void validateSaveInput(T item, Map<String, Object> parameters) {
        Objects.requireNonNull(item, "Item to save cannot be null");
        // Parameters can be null - will be replaced with empty map
    }

    private void validateBatchInput(List<T> items, Map<String, Object> parameters) {
        Objects.requireNonNull(items, "Items list cannot be null");
        // Check for null items in the list
        if (items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Batch cannot contain null items");
        }
    }

    private StreamCollector<T> createStreamCollector(Map<String, Object> parameters) {
        var timeout = getConfigurationValue("streamTimeout", getTimeoutMs());
        var maxItems = getConfigurationValue("streamMaxItems", Integer.MAX_VALUE);
        var collectErrors = getConfigurationValue("streamCollectErrors", true);

        return new StreamCollector<>(timeout, maxItems, collectErrors);
    }

    private DataLoaderException wrapException(String message, String errorCode, Throwable cause, DataLoaderException.ErrorSeverity severity) {
        Map<String, Object> context = Map.of(
                "itemType", itemType.getSimpleName(),
                "idType", idType.getSimpleName(),
                "loaderType", this.getClass().getSimpleName(),
                "operationCount", operationCounter.get(),
                "timestamp", System.currentTimeMillis()
        );
        return new DataLoaderException(message, errorCode, severity, context, cause);
    }

    private Map<String, Object> getFilteredConfiguration() {
        // Filter out sensitive information like passwords
        return configuration.entrySet().stream()
                .filter(entry -> !entry.getKey().toLowerCase().contains("password"))
                .filter(entry -> !entry.getKey().toLowerCase().contains("secret"))
                .filter(entry -> !entry.getKey().toLowerCase().contains("token"))
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);
    }

    /**
     * Cleanup resources when the loader is no longer needed
     */
    public void shutdown() {
        eventListeners.clear();

        // Clear metrics
        metrics.reset();
    }
}