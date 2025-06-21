package it.mathsanalysis.load.resilience.config.structure;

import java.time.Duration;

public record ResilienceConfig(
        boolean circuitBreakerEnabled,
        boolean retryEnabled,
        int maxRetries,
        Duration retryDelay
) {
    public static ResilienceConfig.Builder builder() {
        return new ResilienceConfig.Builder();
    }

    public static ResilienceConfig defaults() {
        return builder().build();
    }

    public static class Builder {
        private boolean circuitBreakerEnabled = true;
        private boolean retryEnabled = true;
        private int maxRetries = 3;
        private Duration retryDelay = Duration.ofSeconds(1);

        public ResilienceConfig.Builder circuitBreakerEnabled(boolean circuitBreakerEnabled) {
            this.circuitBreakerEnabled = circuitBreakerEnabled;
            return this;
        }

        public ResilienceConfig.Builder retryEnabled(boolean retryEnabled) {
            this.retryEnabled = retryEnabled;
            return this;
        }

        public ResilienceConfig.Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public ResilienceConfig.Builder retryDelay(Duration retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public ResilienceConfig build() {
            return new ResilienceConfig(circuitBreakerEnabled, retryEnabled, maxRetries, retryDelay);
        }
    }
}
