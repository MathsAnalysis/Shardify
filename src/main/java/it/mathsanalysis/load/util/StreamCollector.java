package it.mathsanalysis.load.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * High-performance stream collector for reactive operations
 * Collects items from Flow.Publisher with error handling and timeout support
 * 
 * Observer Pattern: Subscribes to reactive streams and collects results
 */
public final class StreamCollector<T> implements Flow.Subscriber<T> {
    
    private final List<T> items = new CopyOnWriteArrayList<>();
    private final List<Exception> errors = new CopyOnWriteArrayList<>();
    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
    
    private volatile boolean completed = false;
    private volatile boolean cancelled = false;
    private volatile Throwable completionError = null;
    
    // Configuration
    private final long timeoutMs;
    private final int maxItems;
    private final boolean collectErrors;
    
    /**
     * Create collector with default settings
     */
    public StreamCollector() {
        this(30000, Integer.MAX_VALUE, true); // 30 second timeout, unlimited items, collect errors
    }
    
    /**
     * Create collector with custom settings
     * @param timeoutMs Timeout in milliseconds for collection
     * @param maxItems Maximum number of items to collect
     * @param collectErrors Whether to collect errors or fail fast
     */
    public StreamCollector(long timeoutMs, int maxItems, boolean collectErrors) {
        this.timeoutMs = timeoutMs;
        this.maxItems = maxItems;
        this.collectErrors = collectErrors;
    }
    
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (subscriptionRef.compareAndSet(null, subscription)) {
            subscription.request(Math.min(maxItems, Long.MAX_VALUE));
        } else {
            subscription.cancel();
        }
    }
    
    @Override
    public void onNext(T item) {
        if (completed || cancelled) {
            return;
        }
        
        if (items.size() >= maxItems) {
            cancel();
            return;
        }
        
        items.add(item);
        
        // Request more items if we haven't reached the limit
        var subscription = subscriptionRef.get();
        if (subscription != null && items.size() < maxItems) {
            subscription.request(1);
        }
    }
    
    @Override
    public void onError(Throwable throwable) {
        if (completed) {
            return;
        }
        
        if (collectErrors && throwable instanceof Exception exception) {
            errors.add(exception);
        } else {
            completionError = throwable;
        }
        
        completed = true;
        completionLatch.countDown();
    }
    
    @Override
    public void onComplete() {
        if (completed) {
            return;
        }
        
        completed = true;
        completionLatch.countDown();
    }
    
    /**
     * Get collected items (blocking until completion)
     * @return List of collected items
     * @throws RuntimeException if collection fails
     */
    public List<T> getItems() {
        waitForCompletion();
        
        if (completionError != null) {
            throw new RuntimeException("Stream collection failed", completionError);
        }
        
        return List.copyOf(items);
    }
    
    /**
     * Get collected items with timeout
     * @param timeout Timeout value
     * @param unit Timeout unit
     * @return List of collected items
     * @throws RuntimeException if collection fails or times out
     */
    public List<T> getItems(long timeout, TimeUnit unit) {
        if (!waitForCompletion(timeout, unit)) {
            cancel();
            throw new RuntimeException("Stream collection timed out after " + timeout + " " + unit);
        }
        
        if (completionError != null) {
            throw new RuntimeException("Stream collection failed", completionError);
        }
        
        return List.copyOf(items);
    }
    
    /**
     * Get collected items without blocking (returns current state)
     * @return List of items collected so far
     */
    public List<T> getItemsNonBlocking() {
        return List.copyOf(items);
    }
    
    /**
     * Get collection errors
     * @return List of errors encountered during collection
     */
    public List<Exception> getErrors() {
        return List.copyOf(errors);
    }
    
    /**
     * Check if collection completed successfully
     * @return true if completed without errors
     */
    public boolean isSuccessful() {
        return completed && errors.isEmpty() && completionError == null;
    }
    
    /**
     * Check if collection is completed
     * @return true if collection is finished (success or failure)
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Check if collection was cancelled
     * @return true if collection was cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Get current item count
     * @return Number of items collected so far
     */
    public int getCurrentItemCount() {
        return items.size();
    }
    
    /**
     * Get current error count
     * @return Number of errors collected so far
     */
    public int getCurrentErrorCount() {
        return errors.size();
    }
    
    /**
     * Cancel the collection
     */
    public void cancel() {
        if (cancelled || completed) {
            return;
        }
        
        cancelled = true;
        var subscription = subscriptionRef.get();
        if (subscription != null) {
            subscription.cancel();
        }
        
        completed = true;
        completionLatch.countDown();
    }
    
    /**
     * Clear collected items and errors
     */
    public void clear() {
        if (!completed) {
            throw new IllegalStateException("Cannot clear while collection is in progress");
        }
        
        items.clear();
        errors.clear();
    }
    
    /**
     * Get collection statistics
     * @return Map of collection statistics
     */
    public java.util.Map<String, Object> getStats() {
        return java.util.Map.of(
            "itemCount", items.size(),
            "errorCount", errors.size(),
            "completed", completed,
            "cancelled", cancelled,
            "successful", isSuccessful(),
            "maxItems", maxItems,
            "timeoutMs", timeoutMs
        );
    }
    
    /**
     * Wait for completion using default timeout
     */
    private void waitForCompletion() {
        waitForCompletion(timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Wait for completion with custom timeout
     * @param timeout Timeout value
     * @param unit Timeout unit
     * @return true if completed within timeout
     */
    private boolean waitForCompletion(long timeout, TimeUnit unit) {
        if (completed) {
            return true;
        }

        try {
            return completionLatch.await(timeout, unit);
        } catch (InterruptedException e) {
            // CRITICAL: Handle interruption properly
            Thread.currentThread().interrupt();
            cancel(); // Cancel collection on interruption
            throw new RuntimeException("Stream collection interrupted", e);
        }
    }
    
    /**
     * Create a new collector with specified timeout
     * @param timeoutMs Timeout in milliseconds
     * @return New StreamCollector instance
     */
    public static <T> StreamCollector<T> withTimeout(long timeoutMs) {
        return new StreamCollector<>(timeoutMs, Integer.MAX_VALUE, true);
    }
    
    /**
     * Create a new collector with specified item limit
     * @param maxItems Maximum number of items to collect
     * @return New StreamCollector instance
     */
    public static <T> StreamCollector<T> withLimit(int maxItems) {
        return new StreamCollector<>(30000, maxItems, true);
    }
    
    /**
     * Create a new collector with fail-fast error handling
     * @return New StreamCollector instance that fails on first error
     */
    public static <T> StreamCollector<T> failFast() {
        return new StreamCollector<>(30000, Integer.MAX_VALUE, false);
    }
    
    /**
     * Create a new collector with custom configuration
     * @param timeoutMs Timeout in milliseconds
     * @param maxItems Maximum number of items
     * @param collectErrors Whether to collect errors
     * @return New StreamCollector instance
     */
    public static <T> StreamCollector<T> create(long timeoutMs, int maxItems, boolean collectErrors) {
        return new StreamCollector<>(timeoutMs, maxItems, collectErrors);
    }

    long getTimeoutMs() {
        return timeoutMs;
    }

    private StreamCollector<T> createStreamCollector(Map<String, Object> parameters) {
        var timeout = getTimeoutMs();
        var maxItems = Integer.MAX_VALUE;
        var collectErrors = true;

        if (parameters != null) {
            var timeoutParam = parameters.get("streamTimeout");
            if (timeoutParam instanceof Long) {
                timeout = (Long) timeoutParam;
            } else if (timeoutParam instanceof Number) {
                timeout = ((Number) timeoutParam).longValue();
            }

            var maxItemsParam = parameters.get("streamMaxItems");
            if (maxItemsParam instanceof Integer) {
                maxItems = (Integer) maxItemsParam;
            } else if (maxItemsParam instanceof Number) {
                maxItems = ((Number) maxItemsParam).intValue();
            }

            var collectErrorsParam = parameters.get("streamCollectErrors");
            if (collectErrorsParam instanceof Boolean) {
                collectErrors = (Boolean) collectErrorsParam;
            }
        }

        return new StreamCollector<>(timeout, maxItems, collectErrors);
    }
}