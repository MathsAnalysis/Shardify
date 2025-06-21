package it.mathsanalysis.load.resilience.config.structure;

import java.time.Duration;

public record MetricsConfig(
        boolean enabled,
        boolean detailedMetrics,
        Duration exportInterval,
        boolean lightweightMode
) {
    public static MetricsConfig.Builder builder() {
        return new MetricsConfig.Builder();
    }

    public static MetricsConfig defaults() {
        return builder().build();
    }

    public static class Builder {
        private boolean enabled = true;
        private boolean detailedMetrics = false;
        private Duration exportInterval = Duration.ofMinutes(5);
        private boolean lightweightMode = false;

        public MetricsConfig.Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public MetricsConfig.Builder detailedMetrics(boolean detailedMetrics) {
            this.detailedMetrics = detailedMetrics;
            return this;
        }

        public MetricsConfig.Builder exportInterval(Duration exportInterval) {
            this.exportInterval = exportInterval;
            return this;
        }

        public MetricsConfig.Builder lightweightMode(boolean lightweightMode) {
            this.lightweightMode = lightweightMode;
            return this;
        }

        public MetricsConfig build() {
            return new MetricsConfig(enabled, detailedMetrics, exportInterval, lightweightMode);
        }
    }
}
