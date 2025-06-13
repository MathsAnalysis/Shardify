package it.mathsanalysis.load.spi.database;

import java.util.Map;
import java.util.Set;
import java.util.Optional;

/**
 * Enhanced document representation interface
 * Abstracts document operations for different document databases with advanced features
 */
public interface Document {
    
    /**
     * Get value by key
     * @param key Field key
     * @return Field value or null if not found
     */
    Object get(String key);
    
    /**
     * Get typed value by key
     * @param key Field key
     * @param type Expected type
     * @return Typed field value or null if not found
     * @throws ClassCastException if value cannot be cast to expected type
     */
    <T> T get(String key, Class<T> type);
    
    /**
     * Get value by key with default
     * @param key Field key
     * @param defaultValue Default value if key not found
     * @return Field value or default value
     */
    Object getOrDefault(String key, Object defaultValue);
    
    /**
     * Get optional value by key
     * @param key Field key
     * @return Optional containing value or empty if not found
     */
    Optional<Object> getOptional(String key);
    
    /**
     * Put key-value pair
     * @param key Field key
     * @param value Field value
     * @return Previous value associated with key, or null
     */
    Object put(String key, Object value);
    
    /**
     * Put all entries from map
     * @param map Map of key-value pairs to add
     */
    void putAll(Map<String, Object> map);
    
    /**
     * Remove field by key
     * @param key Field key to remove
     * @return Previous value associated with key, or null
     */
    Object remove(String key);
    
    /**
     * Check if document contains key
     * @param key Field key
     * @return true if key exists
     */
    boolean containsKey(String key);
    
    /**
     * Check if document contains value
     * @param value Field value
     * @return true if value exists
     */
    boolean containsValue(Object value);
    
    /**
     * Check if document is empty
     * @return true if document has no fields
     */
    boolean isEmpty();
    
    /**
     * Get number of fields
     * @return Field count
     */
    int size();
    
    /**
     * Get all field keys
     * @return Set of field keys
     */
    Set<String> keySet();
    
    /**
     * Get all field values
     * @return Collection of field values
     */
    java.util.Collection<Object> values();
    
    /**
     * Get all key-value entries
     * @return Set of map entries
     */
    Set<Map.Entry<String, Object>> entrySet();
    
    /**
     * Convert document to Map
     * @return Map representation of document
     */
    Map<String, Object> asMap();
    
    /**
     * Convert document to Map with deep copy
     * @return Deep copy Map representation
     */
    Map<String, Object> asMapDeepCopy();
    
    /**
     * Clear all fields
     */
    void clear();
    
    /**
     * Create a copy of this document
     * @return Document copy
     */
    Document copy();
    
    /**
     * Merge with another document
     * @param other Document to merge
     * @param overwrite Whether to overwrite existing fields
     * @return This document for chaining
     */
    Document merge(Document other, boolean overwrite);
    
    /**
     * Get nested document by dot notation
     * @param path Dot-separated path (e.g., "user.address.city")
     * @return Nested value or null if path not found
     */
    Object getNestedValue(String path);
    
    /**
     * Set nested value by dot notation
     * @param path Dot-separated path (e.g., "user.address.city")
     * @param value Value to set
     * @return This document for chaining
     */
    Document setNestedValue(String path, Object value);
    
    /**
     * Check if document has nested path
     * @param path Dot-separated path
     * @return true if path exists
     */
    boolean hasNestedPath(String path);
    
    /**
     * Get document as JSON string
     * @return JSON representation
     */
    String toJson();
    
    /**
     * Get document as pretty JSON string
     * @return Pretty formatted JSON representation
     */
    String toPrettyJson();
    
    /**
     * Get document ID if present
     * @return Document ID or null
     */
    Object getId();
    
    /**
     * Set document ID
     * @param id Document ID
     * @return This document for chaining
     */
    Document setId(Object id);
    
    /**
     * Get document metadata
     * @return Metadata map
     */
    Map<String, Object> getMetadata();
    
    /**
     * Set document metadata
     * @param metadata Metadata to set
     * @return This document for chaining
     */
    Document setMetadata(Map<String, Object> metadata);
}
