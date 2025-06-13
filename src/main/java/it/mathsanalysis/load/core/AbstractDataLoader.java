package it.mathsanalysis.load.core;

import it.mathsanalysis.load.core.result.BatchResult;
import it.mathsanalysis.load.core.result.DebugResult;
import it.mathsanalysis.load.core.result.HealthStatus;
import it.mathsanalysis.load.metrics.PerformanceMetrics;
import it.mathsanalysis.load.util.StreamCollector;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * Abstract base implementation of DataLoader using Template Method pattern.
 *
 * Provides common functionality and algorithms while allowing subclasses
 * to customize specific database operations. This approach ensures consistency
 * across different database implementations while maintaining flexibility.
 *
 * Template Methods Implemented:
 * - Parameter validation and preprocessing
 * - Performance metrics collection
 * - Error handling and wrapping
 * - Async operation coordination
 * - Resource management
 *
 * Subclasses must implement:
 * - doSave: actual database save operation
 * - doSaveBatch: optimized batch save
 * - doFindById: single item retrieval
 * - doInitializeStorage: database structure creation
 * - doHealthCheck: database-specific health checks
 *
 * Thread Safety: This abstract class is thread-safe. Subclasses must ensure
 * their implementations are also thread-safe.
 *
 * @param <T> The type of items being loaded/saved
 * @param <ID> The type of item identifiers
 */
public abstract class AbstractDataLoader<T, ID> implements DataLoader<T, ID> {

    protected final Class<T> itemType;
    protected final Class<ID> idType;
    protected final PerformanceMetrics metrics;
    protected final Map<String, Object> configuration;

    // Configuration constants
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final long DEFAULT_TIMEOUT_MS = 30000;
    private static final boolean DEFAULT_ENABLE_METRICS = true;

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
        this.configuration = Map.copyOf(Objects.requireNonNull(configuration, "Configuration cannot be null"));
        this.metrics = new PerformanceMetrics();
    }

    @Override
    public final T save(T item, Map<String, Object> parameters) {
        // Template method implementation with common algorithm

        // Step 1: Validate input
        validateSaveInput(item, parameters);

        // Step 2: Record operation start
        var startTime = System.nanoTime();

        try {
            // Step 3: Execute database-specific save (hook method)
            var savedItem = doSave(item, parameters);

            // Step 4: Record success metrics
            if (isMetricsEnabled()) {
                metrics.recordOperation("save", System.nanoTime() - startTime);
            }

            return savedItem;

        } catch (Exception e) {
            // Step 5: Handle errors consistently
            if (isMetricsEnabled()) {
                metrics.recordOperation("save_error", System.nanoTime() - startTime);
            }
            throw wrapException("Save operation failed", "SAVE_ERROR", e);
        }
    }

    @Override
    public final CompletableFuture<T> saveAsync(T item, Map<String, Object> parameters) {
        // Validate synchronously to fail fast
        validateSaveInput(item, parameters);

        return CompletableFuture.supplyAsync(() -> save(item, parameters))
            .whenComplete((result, throwable) -> {
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
            throw wrapException("Batch save operation failed", "BATCH_SAVE_ERROR", e);
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
            throw wrapException("Async batch save failed", "ASYNC_BATCH_ERROR", throwable);
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
            throw wrapException("Find by ID operation failed", "FIND_ERROR", e);
        }
    }

    @Override
    public final CompletableFuture<Optional<T>> findByIdAsync(ID id) {
        return CompletableFuture.supplyAsync(() -> findById(id))
            .whenComplete((result, throwable) -> {
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
                throw wrapException("Storage initialization failed", "INIT_ERROR", e);
            }
        });
    }

    @Override
    public final DebugResult getDebugInfo() {
        var loaderInfo = Map.of(
            "itemType", itemType.getSimpleName(),
            "idType", idType.getSimpleName(),
            "implementation", this.getClass().getSimpleName(),
            "configuration", getConfiguration()
        );

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
                return doHealthCheck();
            } catch (Exception e) {
                Map<String, Object> errorMetrics = Map.of(
                    "error", e.getMessage(),
                    "errorType", e.getClass().getSimpleName(),
                    "timestamp", System.currentTimeMillis()
                );
                return HealthStatus.unhealthy("Health check failed: " + e.getMessage(), errorMetrics);
            }
        });
    }

    @Override
    public final Map<String, Object> getConfiguration() {
        return Map.copyOf(configuration);
    }

    // Template methods - must be implemented by subclasses

    /**
     * Database-specific save operation
     *
     * @param item Item to save
     * @param parameters Save parameters
     * @return Saved item with generated fields
     */
    protected abstract T doSave(T item, Map<String, Object> parameters);

    /**
     * Database-specific batch save operation
     *
     * @param items Items to save
     * @param parameters Batch parameters
     * @return Saved items in same order
     */
    protected abstract List<T> doSaveBatch(List<T> items, Map<String, Object> parameters);

    /**
     * Database-specific find by ID operation
     *
     * @param id Item identifier
     * @return Optional containing found item
     */
    protected abstract Optional<T> doFindById(ID id);

    /**
     * Database-specific storage initialization
     *
     * @param parameters Initialization parameters
     */
    protected abstract void doInitializeStorage(Map<String, Object> parameters);

    /**
     * Database-specific health check
     *
     * @return Health status result
     */
    protected abstract HealthStatus doHealthCheck();

    /**
     * Get database connection statistics
     *
     * @return Connection stats map
     */
    protected abstract Map<String, Object> getConnectionStats();

    // Helper methods for subclasses

    protected final Class<T> getItemType() { return itemType; }
    protected final Class<ID> getIdType() { return idType; }
    protected final PerformanceMetrics getMetrics() { return metrics; }

    protected final boolean isMetricsEnabled() {
        return (Boolean) configuration.getOrDefault("enableMetrics", DEFAULT_ENABLE_METRICS);
    }

    protected final int getBatchSize() {
        return (Integer) configuration.getOrDefault("batchSize", DEFAULT_BATCH_SIZE);
    }

    protected final long getTimeoutMs() {
        return (Long) configuration.getOrDefault("timeoutMs", DEFAULT_TIMEOUT_MS);
    }

    // Private helper methods

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
        var timeout = (Long) parameters.getOrDefault("streamTimeout", getTimeoutMs());
        var maxItems = (Integer) parameters.getOrDefault("streamMaxItems", Integer.MAX_VALUE);
        var collectErrors = (Boolean) parameters.getOrDefault("streamCollectErrors", true);

        return new StreamCollector<>(timeout, maxItems, collectErrors);
    }

    private DataLoaderException wrapException(String message, String errorCode, Throwable cause) {
        Map<String, Object> context = Map.of(
            "itemType", itemType.getSimpleName(),
            "idType", idType.getSimpleName(),
            "timestamp", System.currentTimeMillis()
        );
        return new DataLoaderException(message, errorCode, context, cause);
    }
}