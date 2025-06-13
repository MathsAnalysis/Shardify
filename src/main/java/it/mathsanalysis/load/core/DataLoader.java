package it.mathsanalysis.load.core;

import it.mathsanalysis.load.core.result.BatchResult;
import it.mathsanalysis.load.core.result.DebugResult;
import it.mathsanalysis.load.core.result.HealthStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * Core abstraction for high-performance data loading operations.
 *
 * Supports both relational and document-based databases with sync/async operations.
 * Designed for ultra-high performance with zero-copy operations where possible.
 *
 * Design Patterns Used:
 * - Template Method: Common algorithm structure with customizable steps
 * - Strategy: Different implementations for different database types
 * - Builder: Fluent configuration API
 *
 * Performance Features:
 * - Reactive streams for large datasets
 * - Connection pooling with health monitoring
 * - Query caching and optimization
 * - Batch operations with configurable sizes
 * - Zero-copy serialization where supported
 *
 * Thread Safety: All implementations must be thread-safe
 *
 * @param <T> The type of items being loaded/saved
 * @param <ID> The type of item identifiers
 */
public interface DataLoader<T, ID> {

    /**
     * Save single item synchronously
     *
     * High-performance single item persistence with immediate consistency.
     * Uses prepared statements for SQL databases and optimized document insertion
     * for document databases.
     *
     * @param item The item to save (must not be null)
     * @param parameters Optional parameters for the operation
     *                  - SQL: query hints, transaction isolation
     *                  - Document: write concerns, validation rules
     * @return The saved item with generated ID and timestamps if applicable
     * @throws DataLoaderException if save operation fails
     */
    T save(T item, Map<String, Object> parameters);

    /**
     * Save single item asynchronously
     *
     * Non-blocking save operation returning immediately with a CompletableFuture.
     * Ideal for high-throughput applications and Minecraft plugins where
     * blocking the main thread must be avoided.
     *
     * @param item The item to save (must not be null)
     * @param parameters Optional parameters for the operation
     * @return CompletableFuture that completes with the saved item
     */
    CompletableFuture<T> saveAsync(T item, Map<String, Object> parameters);

    /**
     * Save multiple items synchronously in batch
     *
     * Optimized batch operation that groups multiple inserts into single
     * database round-trip. Significantly faster than individual saves for
     * large datasets.
     *
     * Features:
     * - Automatic batching with configurable batch sizes
     * - Transaction management for consistency
     * - Generated key handling for all items
     * - Partial failure handling with detailed error reporting
     *
     * @param items List of items to save (empty list returns empty result)
     * @param parameters Batch operation parameters
     *                  - batchSize: override default batch size
     *                  - transaction: enable/disable transaction wrapping
     * @return List of saved items in same order as input
     * @throws DataLoaderException if batch operation fails
     */
    List<T> saveBatch(List<T> items, Map<String, Object> parameters);

    /**
     * Save items from reactive stream asynchronously
     *
     * Memory-efficient batch save for extremely large datasets using reactive
     * streams. Processes items as they arrive without loading everything into
     * memory simultaneously.
     *
     * Perfect for:
     * - Large file imports
     * - Real-time data streaming
     * - Memory-constrained environments
     *
     * @param items Publisher of items to save (backpressure supported)
     * @param parameters Stream processing parameters
     *                  - bufferSize: items to buffer before database write
     *                  - timeout: maximum time to wait for completion
     *                  - errorHandling: fail-fast or collect-errors strategy
     * @return CompletableFuture of batch operation result with statistics
     */
    CompletableFuture<BatchResult<T>> saveBatchAsync(
            Flow.Publisher<T> items,
            Map<String, Object> parameters
    );

    /**
     * Find item by identifier synchronously
     *
     * High-performance single item retrieval with connection pooling and
     * optional query result caching.
     *
     * @param id The item identifier (null returns empty Optional)
     * @return Optional containing the item if found, empty otherwise
     * @throws DataLoaderException if find operation fails
     */
    Optional<T> findById(ID id);

    /**
     * Find item by identifier asynchronously
     *
     * Non-blocking find operation ideal for web applications and reactive
     * architectures.
     *
     * @param id The item identifier (null completes with empty Optional)
     * @return CompletableFuture of Optional containing the item if found
     */
    CompletableFuture<Optional<T>> findByIdAsync(ID id);

    /**
     * Initialize storage structure (tables/collections) asynchronously
     *
     * Creates necessary database structures including:
     * - Tables with proper column types and constraints (SQL)
     * - Collections with indexes and validation rules (Document)
     * - Optimized indexes for common query patterns
     *
     * Safe to call multiple times - uses IF NOT EXISTS patterns.
     *
     * @param parameters Structure creation parameters
     *                  - SQL: column definitions, indexes, constraints
     *                  - Document: validation schemas, indexes, sharding
     * @return CompletableFuture indicating completion (Void result)
     * @throws DataLoaderException if initialization fails
     */
    CompletableFuture<Void> initializeStorage(Map<String, Object> parameters);

    /**
     * Get comprehensive debug information
     *
     * Provides detailed insights into loader performance and state:
     * - Operation statistics (count, average time, throughput)
     * - Connection pool status and health
     * - Query cache hit rates and statistics
     * - Memory usage and garbage collection impact
     * - Database-specific metrics
     *
     * Essential for performance tuning and troubleshooting.
     *
     * @return Debug result with performance data and statistics
     */
    DebugResult getDebugInfo();

    /**
     * Perform health check on database connectivity
     *
     * Comprehensive health assessment including:
     * - Basic connectivity test
     * - Connection pool validation
     * - Database responsiveness check
     * - Performance threshold validation
     *
     * Used by monitoring systems and load balancers for service health.
     *
     * @return CompletableFuture of health status with detailed metrics
     */
    CompletableFuture<HealthStatus> healthCheck();

    /**
     * Gracefully shutdown the loader and release resources
     *
     * Ensures clean shutdown with proper resource cleanup:
     * - Completes in-flight operations
     * - Closes connection pools
     * - Releases cached resources
     * - Flushes pending metrics
     *
     * Should be called during application shutdown.
     * Loader becomes unusable after shutdown.
     */
    default void shutdown() {
        // Default implementation - can be overridden
    }

    /**
     * Get loader configuration information
     *
     * Returns current configuration settings for inspection and debugging.
     * Useful for configuration validation and troubleshooting.
     *
     * @return Map containing current configuration
     */
    default Map<String, Object> getConfiguration() {
        return Map.of();
    }

    /**
     * Update runtime configuration
     *
     * Allows dynamic reconfiguration of certain settings without restart:
     * - Connection pool sizes
     * - Cache settings
     * - Timeout values
     * - Debug levels
     *
     * Not all settings can be changed at runtime - see implementation docs.
     *
     * @param newConfig Configuration updates to apply
     * @return true if configuration was updated successfully
     */
    default boolean updateConfiguration(Map<String, Object> newConfig) {
        return false; // Default: configuration updates not supported
    }

    /**
     * Exception thrown by DataLoader operations
     *
     * Wraps underlying database exceptions with additional context and
     * standardized error codes for consistent error handling.
     */
    class DataLoaderException extends RuntimeException {
        private final String errorCode;
        private final Map<String, Object> context;

        public DataLoaderException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
            this.context = Map.of();
        }

        public DataLoaderException(String message, String errorCode, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
            this.context = Map.of();
        }

        public DataLoaderException(String message, String errorCode, Map<String, Object> context, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
            this.context = Map.copyOf(context);
        }

        public String getErrorCode() { return errorCode; }
        public Map<String, Object> getContext() { return context; }
    }
}