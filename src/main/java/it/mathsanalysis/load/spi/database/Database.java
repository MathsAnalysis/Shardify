package it.mathsanalysis.load.spi.database;

import java.util.List;
import java.util.Map;

/**
 * Enhanced database interface for document databases
 * Provides comprehensive database management capabilities
 */
public interface Database {
    
    /**
     * Get collection by name
     * @param name Collection name
     * @return Collection instance
     */
    Collection getCollection(String name);
    
    /**
     * Create new collection with options
     * @param name Collection name
     * @param options Creation options (indexes, validation, etc.)
     */
    void createCollection(String name, Map<String, Object> options);
    
    /**
     * Drop collection by name
     * @param name Collection name
     */
    void dropCollection(String name);
    
    /**
     * Check if collection exists
     * @param name Collection name
     * @return true if collection exists
     */
    boolean collectionExists(String name);
    
    /**
     * List all collection names
     * @return List of collection names
     */
    List<String> listCollectionNames();
    
    /**
     * Test database connectivity
     * @return true if database is accessible
     */
    boolean ping();
    
    /**
     * Get database statistics
     * @return Map containing database statistics
     */
    Map<String, Object> getStats();
    
    /**
     * Get database name
     * @return Database name
     */
    String getName();
    
    /**
     * Execute database command
     * @param command Command to execute
     * @return Command result
     */
    Map<String, Object> runCommand(Map<String, Object> command);
    
    /**
     * Create index on collection
     * @param collectionName Collection name
     * @param indexSpec Index specification
     * @param options Index options
     */
    void createIndex(String collectionName, Map<String, Object> indexSpec, Map<String, Object> options);
    
    /**
     * Drop index from collection
     * @param collectionName Collection name
     * @param indexName Index name
     */
    void dropIndex(String collectionName, String indexName);
    
    /**
     * List indexes for collection
     * @param collectionName Collection name
     * @return List of index information
     */
    List<Map<String, Object>> listIndexes(String collectionName);
    
    /**
     * Start database session for transactions
     * @param options Session options
     * @return Session object
     */
    Object startSession(Map<String, Object> options);
    
    /**
     * Get server build information
     * @return Server build info
     */
    Map<String, Object> getBuildInfo();
    
    /**
     * Get server status
     * @return Server status information
     */
    Map<String, Object> getServerStatus();
}