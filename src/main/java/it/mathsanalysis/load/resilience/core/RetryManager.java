package it.mathsanalysis.load.resilience.core;

import it.mathsanalysis.load.resilience.exception.DataLoaderException;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.function.Predicate;

/**
 * Retry manager to handle retry logic with configurable parameters.
 */
public final class RetryManager {

    private final RetryConfig defaultConfig;

    public RetryManager() {
        this.defaultConfig = RetryConfig.builder()
                .maxAttempts(3)
                .baseDelay(Duration.ofMillis(1000))
                .maxDelay(Duration.ofSeconds(30))
                .multiplier(2.0)
                .retryIf(exception -> exception instanceof DataLoaderException dle && dle.isRetryable())
                .build();
    }

    public <T> T execute(Supplier<T> action) {
        return executeWithConfig(action, defaultConfig);
    }

    public <T> T executeWithConfig(Supplier<T> action, RetryConfig config) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;

                if (attempt == config.maxAttempts() || !config.retryPredicate().test(e)) {
                    break;
                }

                // Calculate delay with exponential backoff
                var delay = calculateDelay(attempt, config);
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        throw new RuntimeException("Retry failed after " + config.maxAttempts() + " attempts", lastException);
    }

    private Duration calculateDelay(int attempt, RetryConfig config) {
        var delay = config.baseDelay().toMillis() * Math.pow(config.multiplier(), attempt - 1);
        return Duration.ofMillis(Math.min((long) delay, config.maxDelay().toMillis()));
    }

    public record RetryConfig(
            int maxAttempts,
            Duration baseDelay,
            Duration maxDelay,
            double multiplier,
            Predicate<Exception> retryPredicate
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int maxAttempts = 3;
            private Duration baseDelay = Duration.ofMillis(1000);
            private Duration maxDelay = Duration.ofSeconds(30);
            private double multiplier = 2.0;
            private Predicate<Exception> retryPredicate = e -> true;

            public Builder maxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
                return this;
            }

            public Builder baseDelay(Duration baseDelay) {
                this.baseDelay = baseDelay;
                return this;
            }

            public Builder maxDelay(Duration maxDelay) {
                this.maxDelay = maxDelay;
                return this;
            }

            public Builder multiplier(double multiplier) {
                this.multiplier = multiplier;
                return this;
            }

            public Builder retryIf(Predicate<Exception> predicate) {
                this.retryPredicate = predicate;
                return this;
            }

            public RetryConfig build() {
                return new RetryConfig(maxAttempts, baseDelay, maxDelay, multiplier, retryPredicate);
            }
        }
    }
}