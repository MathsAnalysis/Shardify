package it.mathsanalysis.load.spi.database;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Generic result set interface
 * Abstracts result iteration and value extraction from SQL queries
 * 
 * Abstraction Pattern: Unified interface for SQL result sets
 */
public interface ResultSet extends AutoCloseable {
    
    /**
     * Move cursor to next row
     * @return true if there is a next row, false otherwise
     * @throws RuntimeException if navigation fails
     */
    boolean next();
    
    /**
     * Get value by column name
     * @param columnName Name of the column
     * @return Column value or null
     * @throws RuntimeException if column cannot be accessed
     */
    Object getValue(String columnName);
    
    /**
     * Get value by column index (1-based)
     * @param columnIndex Index of the column (starts from 1)
     * @return Column value or null
     * @throws RuntimeException if column cannot be accessed
     */
    Object getValue(int columnIndex);
    
    /**
     * Get typed value by column name
     * @param columnName Name of the column
     * @param type Expected type of the value
     * @return Typed column value or null
     * @throws RuntimeException if column cannot be accessed or cast
     */
    <T> T getValue(String columnName, Class<T> type);
    
    /**
     * Get typed value by column index
     * @param columnIndex Index of the column (starts from 1)
     * @param type Expected type of the value
     * @return Typed column value or null
     * @throws RuntimeException if column cannot be accessed or cast
     */
    <T> T getValue(int columnIndex, Class<T> type);
    
    /**
     * Get string value by column name
     * @param columnName Name of the column
     * @return String value or null
     */
    String getString(String columnName);
    
    /**
     * Get integer value by column name
     * @param columnName Name of the column
     * @return Integer value or null
     */
    Integer getInt(String columnName);
    
    /**
     * Get long value by column name
     * @param columnName Name of the column
     * @return Long value or null
     */
    Long getLong(String columnName);
    
    /**
     * Get double value by column name
     * @param columnName Name of the column
     * @return Double value or null
     */
    Double getDouble(String columnName);
    
    /**
     * Get boolean value by column name
     * @param columnName Name of the column
     * @return Boolean value or null
     */
    Boolean getBoolean(String columnName);
    
    /**
     * Get BigDecimal value by column name
     * @param columnName Name of the column
     * @return BigDecimal value or null
     */
    BigDecimal getBigDecimal(String columnName);
    
    /**
     * Get LocalDate value by column name
     * @param columnName Name of the column
     * @return LocalDate value or null
     */
    LocalDate getLocalDate(String columnName);
    
    /**
     * Get LocalDateTime value by column name
     * @param columnName Name of the column
     * @return LocalDateTime value or null
     */
    LocalDateTime getLocalDateTime(String columnName);
    
    /**
     * Check if last retrieved value was null
     * @return true if last value was null
     */
    boolean wasNull();
    
    /**
     * Get metadata about the result set
     * @return Result set metadata
     */
    ResultSetMetadata getMetadata();
    
    /**
     * Check if result set is empty
     * @return true if result set has no rows
     */
    boolean isEmpty();
    
    /**
     * Get current row number (1-based)
     * @return Current row number
     */
    int getRow();
    
    /**
     * Close the result set and release resources
     */
    @Override
    void close();
}