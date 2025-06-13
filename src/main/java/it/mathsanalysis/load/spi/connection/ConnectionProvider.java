package it.mathsanalysis.load.spi.connection;

import it.mathsanalysis.load.spi.database.Connection;

import java.util.Map;

/**
 * Enhanced provider for SQL database connections
 * Handles connection pooling and lifecycle management with health monitoring
 */
public interface ConnectionProvider {

    /**
     * Get a connection from the pool
     * @return Active database connection
     */
    Connection getConnection();

    /**
     * Get connection pool statistics
     * @return Map containing pool metrics
     */
    Map<String, Object> getConnectionStats();

    /**
     * Check if connection provider is healthy
     * @param timeoutSeconds Timeout for health check
     * @return true if healthy
     */
    boolean isHealthy(int timeoutSeconds);

    /**
     * Get number of active connections
     * @return Active connection count
     */
    int getActiveConnections();

    /**
     * Get maximum pool size
     * @return Maximum pool size
     */
    int getMaxPoolSize();

    /**
     * Get number of idle connections
     * @return Idle connection count
     */
    int getIdleConnections();

    /**
     * Get connection provider configuration
     * @return Configuration map
     */
    Map<String, Object> getConfiguration();

    /**
     * Update connection pool size
     * @param newMaxSize New maximum pool size
     * @param newMinIdle New minimum idle connections
     */
    void adjustPoolSize(int newMaxSize, int newMinIdle);

    /**
     * Close all connections and cleanup resources
     */
    void close();
}