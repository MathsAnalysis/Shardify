package it.mathsanalysis.load.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Enhanced performance metrics collection with resetStats method
 * Thread-safe with minimal overhead using atomic operations
 */
public final class PerformanceMetrics {

    private final Map<String, LongAdder> operationCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> operationTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> batchSizes = new ConcurrentHashMap<>();
    private final AtomicLong lastOperationTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

    /**
     * Record a single operation execution
     * @param operationType Type of operation (save, find, etc.)
     * @param durationNanos Execution time in nanoseconds
     */
    public void recordOperation(String operationType, long durationNanos) {
        operationCounts.computeIfAbsent(operationType, k -> new LongAdder()).increment();
        operationTimes.computeIfAbsent(operationType, k -> new LongAdder()).add(durationNanos);
        lastOperationTime.set(System.currentTimeMillis());
    }

    /**
     * Record a batch operation execution
     * @param operationType Type of operation
     * @param batchSize Number of items in batch
     * @param durationNanos Total execution time in nanoseconds
     */
    public void recordBatchOperation(String operationType, int batchSize, long durationNanos) {
        recordOperation(operationType, durationNanos);
        batchSizes.computeIfAbsent(operationType, k -> new AtomicLong()).addAndGet(batchSize);
    }

    /**
     * Get comprehensive performance statistics
     * @return Map containing all performance metrics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        operationCounts.forEach((operation, count) -> {
            var totalTime = operationTimes.get(operation).sum();
            var operationCount = count.sum();
            var averageTime = operationCount > 0 ? totalTime / operationCount : 0;

            stats.put(operation + "_count", operationCount);
            stats.put(operation + "_total_time_ns", totalTime);
            stats.put(operation + "_avg_time_ns", averageTime);

            var batchSize = batchSizes.get(operation);
            if (batchSize != null) {
                stats.put(operation + "_total_items", batchSize.get());
                stats.put(operation + "_avg_batch_size", batchSize.get() / operationCount);
            }
        });

        stats.put("last_operation_timestamp", lastOperationTime.get());
        stats.put("operations_per_second", calculateOperationsPerSecond());
        stats.put("uptime_ms", System.currentTimeMillis() - startTime.get());

        return stats;
    }

    /**
     * Get latest performance snapshot
     * @return Recent performance data
     */
    public Map<String, Object> getLatestMetrics() {
        return Map.of(
                "last_operation", lastOperationTime.get(),
                "total_operations", getTotalOperations(),
                "ops_per_second", calculateOperationsPerSecond(),
                "uptime_ms", System.currentTimeMillis() - startTime.get()
        );
    }

    /**
     * Reset all statistics - NEW METHOD
     */
    public void resetStats() {
        operationCounts.clear();
        operationTimes.clear();
        batchSizes.clear();
        startTime.set(System.currentTimeMillis());
        lastOperationTime.set(System.currentTimeMillis());
    }

    private long getTotalOperations() {
        return operationCounts.values().stream()
                .mapToLong(LongAdder::sum)
                .sum();
    }

    private double calculateOperationsPerSecond() {
        var totalOps = getTotalOperations();
        var uptimeMs = System.currentTimeMillis() - startTime.get();
        return uptimeMs > 0 ? (totalOps * 1000.0) / uptimeMs : 0.0;
    }
}