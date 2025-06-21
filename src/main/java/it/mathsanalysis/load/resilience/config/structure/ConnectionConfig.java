package it.mathsanalysis.load.resilience.config.structure;

import java.time.Duration;

public record ConnectionConfig(
        int maxPoolSize,
        int minIdle,
        Duration connectionTimeout,
        Duration idleTimeout,
        Duration maxLifetime,
        boolean leakDetection,
        Duration leakDetectionThreshold
) {

    public static ConnectionConfig.Builder builder() {
        return new ConnectionConfig.Builder();
    }

    public static ConnectionConfig defaults() {
        return builder().build();
    }

    public static class Builder {
        private int maxPoolSize = 20;
        private int minIdle = 5;
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private Duration maxLifetime = Duration.ofMinutes(30);
        private boolean leakDetection = true;
        private Duration leakDetectionThreshold = Duration.ofMinutes(1);

        public ConnectionConfig.Builder maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public ConnectionConfig.Builder minIdle(int minIdle) {
            this.minIdle = minIdle;
            return this;
        }

        public ConnectionConfig.Builder connectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public ConnectionConfig.Builder idleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        public ConnectionConfig.Builder maxLifetime(Duration maxLifetime) {
            this.maxLifetime = maxLifetime;
            return this;
        }

        public ConnectionConfig.Builder leakDetection(boolean leakDetection) {
            this.leakDetection = leakDetection;
            return this;
        }

        public ConnectionConfig.Builder leakDetectionThreshold(Duration leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
            return this;
        }

        public ConnectionConfig build() {
            return new ConnectionConfig(maxPoolSize, minIdle, connectionTimeout,
                    idleTimeout, maxLifetime, leakDetection, leakDetectionThreshold);
        }
    }
}
