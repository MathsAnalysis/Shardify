package it.mathsanalysis.load.spi.database;

/**
 * Generic database connection interface
 * Abstracts common connection operations for SQL databases
 * 
 * Abstraction Pattern: Provides unified interface for different SQL databases
 */
public interface Connection extends AutoCloseable {
    
    /**
     * Prepare a statement for execution
     * @param sql SQL query string
     * @return Prepared statement
     * @throws RuntimeException if statement cannot be prepared
     */
    PreparedStatement prepareStatement(String sql);
    
    /**
     * Prepare a statement that can return generated keys
     * @param sql SQL query string
     * @param returnGeneratedKeys Whether to return generated keys
     * @return Prepared statement
     * @throws RuntimeException if statement cannot be prepared
     */
    PreparedStatement prepareStatement(String sql, boolean returnGeneratedKeys);
    
    /**
     * Test if connection is valid
     * @param timeout Timeout in seconds
     * @return true if connection is valid
     */
    boolean isValid(int timeout);
    
    /**
     * Start a transaction
     * @throws RuntimeException if transaction cannot be started
     */
    void beginTransaction();
    
    /**
     * Commit current transaction
     * @throws RuntimeException if commit fails
     */
    void commit();
    
    /**
     * Rollback current transaction
     * @throws RuntimeException if rollback fails
     */
    void rollback();
    
    /**
     * Set auto-commit mode
     * @param autoCommit true to enable auto-commit
     * @throws RuntimeException if auto-commit cannot be set
     */
    void setAutoCommit(boolean autoCommit);
    
    /**
     * Get auto-commit mode
     * @return true if auto-commit is enabled
     */
    boolean getAutoCommit();
    
    /**
     * Close the connection
     */
    @Override
    void close();
}