package it.mathsanalysis.load.resilience.core;

import it.mathsanalysis.load.resilience.exception.ConnectionException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Circuit Breaker implementation for database operations
 * Integrated for preventing cascading failures
 */
public final class CircuitBreakerManager {
    
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final CircuitBreakerConfig defaultConfig;
    
    public CircuitBreakerManager() {
        this.defaultConfig = CircuitBreakerConfig.builder()
            .failureThreshold(5)
            .recoveryTimeout(Duration.ofSeconds(30))
            .successThreshold(3)
            .build();
    }
    
    public <T> T execute(String operation, Supplier<T> action) {
        var breaker = getOrCreateCircuitBreaker(operation);
        return breaker.execute(action);
    }
    
    private CircuitBreaker getOrCreateCircuitBreaker(String operation) {
        return breakers.computeIfAbsent(operation, 
            key -> new CircuitBreaker(key, defaultConfig));
    }
    
    public Map<String, CircuitBreakerStats> getAllStats() {
        return breakers.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getStats()
        ));
    }
    
    public static class CircuitBreaker {
        private final String name;
        private final CircuitBreakerConfig config;
        private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private volatile Instant lastFailureTime = Instant.MIN;
        
        public enum State { CLOSED, OPEN, HALF_OPEN }
        
        CircuitBreaker(String name, CircuitBreakerConfig config) {
            this.name = name;
            this.config = config;
        }
        
        public <T> T execute(Supplier<T> action) {
            var currentState = state.get();
            
            switch (currentState) {
                case OPEN -> {
                    if (shouldAttemptReset()) {
                        state.compareAndSet(State.OPEN, State.HALF_OPEN);
                        return executeInHalfOpenState(action);
                    } else {
                        throw new ConnectionException("Circuit breaker is OPEN for operation: " + name, 
                            Map.of("circuitBreaker", name, "state", "OPEN"), null);
                    }
                }
                case HALF_OPEN -> {
                    return executeInHalfOpenState(action);
                }
                case CLOSED -> {
                    return executeInClosedState(action);
                }
                default -> throw new IllegalStateException("Unknown circuit breaker state: " + currentState);
            }
        }
        
        private <T> T executeInClosedState(Supplier<T> action) {
            try {
                var result = action.get();
                resetFailureCount();
                return result;
            } catch (Exception e) {
                recordFailure();
                if (failureCount.get() >= config.failureThreshold()) {
                    state.set(State.OPEN);
                    lastFailureTime = Instant.now();
                }
                throw e;
            }
        }
        
        private <T> T executeInHalfOpenState(Supplier<T> action) {
            try {
                var result = action.get();
                recordSuccess();
                if (successCount.get() >= config.successThreshold()) {
                    state.set(State.CLOSED);
                    resetCounters();
                }
                return result;
            } catch (Exception e) {
                state.set(State.OPEN);
                lastFailureTime = Instant.now();
                resetCounters();
                throw e;
            }
        }
        
        private boolean shouldAttemptReset() {
            return Instant.now().isAfter(lastFailureTime.plus(config.recoveryTimeout()));
        }
        
        private void recordFailure() {
            failureCount.incrementAndGet();
            successCount.set(0);
        }
        
        private void recordSuccess() {
            successCount.incrementAndGet();
        }
        
        private void resetFailureCount() {
            failureCount.set(0);
        }
        
        private void resetCounters() {
            failureCount.set(0);
            successCount.set(0);
        }
        
        public CircuitBreakerStats getStats() {
            return new CircuitBreakerStats(
                name,
                state.get(),
                failureCount.get(),
                successCount.get(),
                lastFailureTime
            );
        }
    }
    
    public record CircuitBreakerConfig(
        int failureThreshold,
        Duration recoveryTimeout,
        int successThreshold
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private int failureThreshold = 5;
            private Duration recoveryTimeout = Duration.ofSeconds(30);
            private int successThreshold = 3;
            
            public Builder failureThreshold(int failureThreshold) {
                this.failureThreshold = failureThreshold;
                return this;
            }
            
            public Builder recoveryTimeout(Duration recoveryTimeout) {
                this.recoveryTimeout = recoveryTimeout;
                return this;
            }
            
            public Builder successThreshold(int successThreshold) {
                this.successThreshold = successThreshold;
                return this;
            }
            
            public CircuitBreakerConfig build() {
                return new CircuitBreakerConfig(failureThreshold, recoveryTimeout, successThreshold);
            }
        }
    }
    
    public record CircuitBreakerStats(
        String name,
        CircuitBreaker.State state,
        int failureCount,
        int successCount,
        Instant lastFailureTime
    ) {}
}