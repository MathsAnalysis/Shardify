package it.mathsanalysis.load.spi.mapping;

import it.mathsanalysis.load.spi.database.ResultSet;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maps between objects and SQL result sets
 * High-performance mapping with minimal allocations
 * 
 * Strategy Pattern: Different mapping strategies for different object types
 */
public interface ItemMapper<T> {
    
    /**
     * Map item from SQL result set
     * @param resultSet SQL result set positioned at current row
     * @return Mapped item
     * @throws RuntimeException if mapping fails
     */
    T mapFromResultSet(ResultSet resultSet);
    
    /**
     * Map item from result set with existing item context
     * Used for updates where generated keys need to be merged
     * @param resultSet SQL result set positioned at current row
     * @param existingItem Existing item for reference
     * @return Updated item with generated values
     * @throws RuntimeException if mapping fails
     */
    T mapFromResultSet(ResultSet resultSet, T existingItem);
    
    /**
     * Map multiple items from batch result set
     * @param resultSet Batch result set with generated keys
     * @param originalItems Original items for reference
     * @return List of updated items with generated values
     * @throws RuntimeException if mapping fails
     */
    List<T> mapBatchFromResultSet(ResultSet resultSet, List<T> originalItems);
    
    /**
     * Extract parameters from item for query binding
     * @param item Item to extract parameters from
     * @return List of query parameters in correct order
     * @throws RuntimeException if parameter extraction fails
     */
    List<Object> extractParameters(T item);
    
    /**
     * Extract parameters with field mapping
     * @param item Item to extract parameters from
     * @return Map of field names to values
     * @throws RuntimeException if parameter extraction fails
     */
    Map<String, Object> extractParameterMap(T item);
    
    /**
     * Extract only specific fields from item
     * @param item Item to extract parameters from
     * @param fieldNames Names of fields to extract
     * @return List of parameters for specified fields
     * @throws RuntimeException if parameter extraction fails
     */
    List<Object> extractParameters(T item, List<String> fieldNames);
    
    /**
     * Get field names for this mapper
     * @return List of field names in mapping order
     */
    List<String> getFieldNames();
    
    /**
     * Get primary key field name
     * @return Primary key field name
     */
    String getPrimaryKeyField();
    
    /**
     * Get item class that this mapper handles
     * @return Item class
     */
    Class<T> getItemClass();
    
    /**
     * Check if field is nullable
     * @param fieldName Field name to check
     * @return true if field can be null
     */
    boolean isFieldNullable(String fieldName);
    
    /**
     * Get field type
     * @param fieldName Field name
     * @return Field type class
     */
    Class<?> getFieldType(String fieldName);
    
    /**
     * Get primary key value from item
     * @param item Item to get primary key from
     * @return Primary key value
     */
    Object getPrimaryKeyValue(T item);
    
    /**
     * Set primary key value on item
     * @param item Item to set primary key on
     * @param primaryKeyValue Primary key value
     * @return Item with updated primary key
     */
    T setPrimaryKeyValue(T item, Object primaryKeyValue);
    
    /**
     * Create new instance of item class
     * @return New instance
     * @throws RuntimeException if instance cannot be created
     */
    T createNewInstance();
    
    /**
     * Create new instance with specific values
     * @param values Map of field values
     * @return New instance with values set
     * @throws RuntimeException if instance cannot be created
     */
    T createNewInstance(Map<String, Object> values);
    
    /**
     * Check if item has generated key
     * @param item Item to check
     * @return true if item has a generated key
     */
    boolean hasGeneratedKey(T item);
    
    /**
     * Validate item before mapping
     * @param item Item to validate
     * @return List of validation errors (empty if valid)
     */
    List<String> validateItem(T item);
    
    /**
     * Get mapping statistics
     * @return Map of mapping performance statistics
     */
    Map<String, Object> getMappingStats();
    
    /**
     * Reset mapping statistics
     */
    void resetMappingStats();
}