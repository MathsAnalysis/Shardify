package it.mathsanalysis.load.plugin.core;

import it.mathsanalysis.load.plugin.structure.DataLoaderPlugin;
import it.mathsanalysis.load.plugin.structure.PluginContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PluginManager {

    private final List<DataLoaderPlugin> plugins = new CopyOnWriteArrayList<>();
    private PluginContext context;

    public void initialize(PluginContext context) {
        this.context = context;
    }

    public void registerPlugin(DataLoaderPlugin plugin) {
        if (context == null) {
            throw new IllegalStateException("PluginManager not initialized");
        }

        plugin.initialize(context);
        plugins.add(plugin);
    }

    public void unregisterPlugin(DataLoaderPlugin plugin) {
        if (plugins.remove(plugin)) {
            plugin.shutdown();
        }
    }

    public <T> void executeBeforeSave(T item, Map<String, Object> context) {
        executeHook("beforeSave", plugin -> plugin.beforeSave(item, context));
    }

    public <T> void executeAfterSave(T item, T result, Map<String, Object> context) {
        executeHook("afterSave", plugin -> plugin.afterSave(item, result, context));
    }

    public <T> void executeBeforeDelete(T item, Map<String, Object> context) {
        executeHook("beforeDelete", plugin -> plugin.beforeDelete(item, context));
    }

    public <T> void executeAfterDelete(T item, Map<String, Object> context) {
        executeHook("afterDelete", plugin -> plugin.afterDelete(item, context));
    }

    public <T> void executeOnError(T item, Exception error, Map<String, Object> context) {
        executeHook("onError", plugin -> plugin.onError(item, error, context));
    }

    private void executeHook(String hookName, PluginAction action) {
        plugins.parallelStream().forEach(plugin -> {
            try {
                action.execute(plugin);
            } catch (Exception e) {
                System.err.println("Plugin " + plugin.getName() + " failed on hook " + hookName + ": " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        plugins.forEach(plugin -> {
            try {
                plugin.shutdown();
            } catch (Exception e) {
                System.err.println("Error shutting down plugin " + plugin.getName() + ": " + e.getMessage());
            }
        });
        plugins.clear();
    }

    public List<DataLoaderPlugin> getPlugins() {
        return List.copyOf(plugins);
    }

    @FunctionalInterface
    private interface PluginAction {
        void execute(DataLoaderPlugin plugin) throws Exception;
    }
}
