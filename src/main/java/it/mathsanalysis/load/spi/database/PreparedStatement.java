
package it.mathsanalysis.load.spi.database;

/**
 * Generic prepared statement interface
 * Abstracts parameter binding and execution for SQL statements
 * 
 * Abstraction Pattern: Unified interface for SQL prepared statements
 */
public interface PreparedStatement extends AutoCloseable {

    /**
     * Set parameter by index (1-based)
     * @param index Parameter index (starts from 1)
     * @param value Parameter value
     * @throws RuntimeException if parameter cannot be set
     */
    void setParameter(int index, Object value);
    
    /**
     * Set parameter with auto-incrementing index
     * Useful for sequential parameter setting
     * @param value Parameter value
     * @throws RuntimeException if parameter cannot be set
     */
    void setParameter(Object value);
    
    /**
     * Execute update statement (INSERT, UPDATE, DELETE)
     * @return Number of affected rows
     * @throws RuntimeException if execution fails
     */
    int executeUpdate();
    
    /**
     * Execute query statement (SELECT)
     * @return Result set containing query results
     * @throws RuntimeException if execution fails
     */
    ResultSet executeQuery();
    
    /**
     * Get generated keys from last insert operation
     * @return Result set containing generated keys
     * @throws RuntimeException if keys cannot be retrieved
     */
    ResultSet getGeneratedKeys();
    
    /**
     * Add current parameter set to batch
     * @throws RuntimeException if batch addition fails
     */
    void addBatch();
    
    /**
     * Execute all batched statements
     * @return Array of update counts for each batched statement
     * @throws RuntimeException if batch execution fails
     */
    int[] executeBatch();
    
    /**
     * Clear all batched statements
     * @throws RuntimeException if batch cannot be cleared
     */
    void clearBatch();
    
    /**
     * Reset parameter index for auto-incrementing setParameter calls
     */
    void resetParameterIndex();
    
    /**
     * Get current parameter count
     * @return Number of parameters set
     */
    int getParameterCount();
    
    /**
     * Close the statement and release resources
     */
    @Override
    void close();
}