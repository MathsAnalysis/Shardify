package it.mathsanalysis.load.spi.database;

import java.util.List;

/**
 * Metadata interface for result sets
 * Provides information about columns and structure
 */
public interface ResultSetMetadata {
    
    /**
     * Get number of columns in result set
     * @return Column count
     */
    int getColumnCount();
    
    /**
     * Get column name by index (1-based)
     * @param columnIndex Index of the column (starts from 1)
     * @return Column name
     */
    String getColumnName(int columnIndex);
    
    /**
     * Get column type by index (1-based)
     * @param columnIndex Index of the column (starts from 1)
     * @return Column type name
     */
    String getColumnType(int columnIndex);
    
    /**
     * Get all column names
     * @return List of column names in order
     */
    List<String> getColumnNames();
    
    /**
     * Check if column is nullable
     * @param columnIndex Index of the column (starts from 1)
     * @return true if column can contain null values
     */
    boolean isNullable(int columnIndex);
    
    /**
     * Get column precision
     * @param columnIndex Index of the column (starts from 1)
     * @return Column precision
     */
    int getPrecision(int columnIndex);
    
    /**
     * Get column scale
     * @param columnIndex Index of the column (starts from 1)
     * @return Column scale
     */
    int getScale(int columnIndex);
}