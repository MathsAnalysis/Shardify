// ===== src/main/java/it/mathsanalysis/load/plugin/DataLoaderPlugin.java =====
package it.mathsanalysis.load.plugin.structure;

import java.util.Map;
import java.util.Set;


public interface DataLoaderPlugin {
    
    /**
     * Name the plugin
     */
    String getName();
    
    /**
     * Version of the plugin
     */
    String getVersion();
    
    /**
     * Supported databases by the plugin
     */
    Set<String> getSupportedDatabases();
    
    /**
     * Initialize the plugin with the given context.
     */
    void initialize(PluginContext context);
    
    /**
     * Shutdown the plugin and release resources.
     */
    void shutdown();
    
    /**
     * Hook executed before the save operation.
     */
    default <T> void beforeSave(T item, Map<String, Object> context) {}
    
    /**
     * Hook executed after the save operation.
     */
    default <T> void afterSave(T item, T result, Map<String, Object> context) {}
    
    /**
     * Hook executed before the delete operation.
     */
    default <T> void beforeDelete(T item, Map<String, Object> context) {}
    
    /**
     * Hook executed after the delete operation.
     */
    default <T> void afterDelete(T item, Map<String, Object> context) {}
    
    /**
     * Hook executed when an error occurs during the operation.
     */
    default <T> void onError(T item, Exception error, Map<String, Object> context) {}
    
    /**
     * Returns the configuration of the plugin.
     */
    default Map<String, Object> getConfiguration() {
        return Map.of();
    }
    
    /**
     * Checks if the plugin supports the given operation and item type.
     */
    default boolean supports(String operation, Class<?> itemType) {
        return true;
    }
}
