package it.mathsanalysis.load.plugin.core;

import it.mathsanalysis.load.plugin.structure.DataLoaderPlugin;
import it.mathsanalysis.load.plugin.structure.PluginContext;

import java.util.Map;
import java.util.Set;


public final class LoggingPlugin implements DataLoaderPlugin {

    @Override
    public String getName() {
        return "OperationLogging";
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
        System.out.println("LoggingPlugin initialized for " + context.itemType().getSimpleName());
    }

    @Override
    public void shutdown() {
        System.out.println("LoggingPlugin shutdown");
    }

    @Override
    public <T> void beforeSave(T item, Map<String, Object> context) {
        System.out.println("About to save: " + item.getClass().getSimpleName());
    }

    @Override
    public <T> void afterSave(T item, T result, Map<String, Object> context) {
        System.out.println("Successfully saved: " + result.getClass().getSimpleName());
    }

    @Override
    public <T> void onError(T item, Exception error, Map<String, Object> context) {
        System.err.println("Error saving " + item.getClass().getSimpleName() + ": " + error.getMessage());
    }
}
