// ===== spi/connection/DocumentConnectionProvider.java =====
package it.mathsanalysis.load.spi.connection;

import it.mathsanalysis.load.spi.database.Collection;
import it.mathsanalysis.load.spi.database.Database;
import it.mathsanalysis.load.spi.database.Document;

import java.util.List;
import java.util.Map;

/**
 * Enhanced provider for document database connections
 * Handles database and collection access with advanced features
 */
public interface DocumentConnectionProvider {

    /**
     * Get database instance
     * @return Database instance
     */
    Database getDatabase();

    /**
     * Get collection by name
     * @param collectionName Name of the collection
     * @return Collection instance
     */
    Collection getCollection(String collectionName);

    /**
     * Get connection statistics
     * @return Map containing connection metrics
     */
    Map<String, Object> getConnectionStats();

    /**
     * Check if connection is healthy
     * @return true if connection is healthy
     */
    boolean isHealthy();

    /**
     * Get database name
     * @return Database name
     */
    String getDatabaseName();

    /**
     * Check if collection exists
     * @param collectionName Collection name
     * @return true if collection exists
     */
    boolean collectionExists(String collectionName);

    /**
     * List all collection names
     * @return List of collection names
     */
    java.util.List<String> listCollectionNames();

    /**
     * Get connection string (for debugging)
     * @return Connection string
     */
    String getConnectionString();

    /**
     * Start database session for transactions
     * @param options Session options
     * @return Session object
     */
    Object startSession(Map<String, Object> options);

    /**
     * Get GridFS bucket for file operations
     * @return GridFS bucket
     */
    Object getGridFS();

    /**
     * Close connection and cleanup resources
     */
    void close();
}
