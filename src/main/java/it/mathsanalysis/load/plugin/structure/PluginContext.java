package it.mathsanalysis.load.plugin.structure;

import it.mathsanalysis.load.metrics.PerformanceMetrics;
import it.mathsanalysis.load.resilience.config.central.DataLoaderConfiguration;

import java.util.Map;

public record PluginContext(
        DataLoaderConfiguration configuration,
        PerformanceMetrics metrics,
        Map<String, Object> sharedData,
        String loaderType,
        Class<?> itemType
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DataLoaderConfiguration configuration;
        private PerformanceMetrics metrics;
        private Map<String, Object> sharedData = Map.of();
        private String loaderType;
        private Class<?> itemType;

        public Builder configuration(DataLoaderConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder metrics(PerformanceMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder sharedData(Map<String, Object> sharedData) {
            this.sharedData = sharedData;
            return this;
        }

        public Builder loaderType(String loaderType) {
            this.loaderType = loaderType;
            return this;
        }

        public Builder itemType(Class<?> itemType) {
            this.itemType = itemType;
            return this;
        }

        public PluginContext build() {
            return new PluginContext(configuration, metrics, sharedData, loaderType, itemType);
        }
    }
}
