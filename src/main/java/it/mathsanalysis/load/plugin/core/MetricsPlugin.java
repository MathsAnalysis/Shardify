package it.mathsanalysis.load.plugin.core;

import it.mathsanalysis.load.plugin.structure.DataLoaderPlugin;
import it.mathsanalysis.load.plugin.structure.PluginContext;

import java.util.Map;
import java.util.Set;

public final class MetricsPlugin implements DataLoaderPlugin {

    private final Map<String, Long> operationCounts = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "Shardify Metrics";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public Set<String> getSupportedDatabases() {
        return Set.of("*");
    }

    @Override
    public void initialize(PluginContext context) {
        operationCounts.put("saves", 0L);
        operationCounts.put("deletes", 0L);
        operationCounts.put("errors", 0L);
    }

    @Override
    public void shutdown() {
        System.out.println("Final metrics: " + operationCounts);
    }

    @Override
    public <T> void afterSave(T item, T result, Map<String, Object> context) {
        operationCounts.merge("saves", 1L, Long::sum);
    }

    @Override
    public <T> void afterDelete(T item, Map<String, Object> context) {
        operationCounts.merge("deletes", 1L, Long::sum);
    }

    @Override
    public <T> void onError(T item, Exception error, Map<String, Object> context) {
        operationCounts.merge("errors", 1L, Long::sum);
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return Map.of("metrics", operationCounts);
    }
}
