package it.mathsanalysis.load.spi.serialization;

import it.mathsanalysis.load.spi.database.Document;

import java.util.List;
import java.util.Map;

/**
 * Serializes objects to/from document format
 * Optimized for document databases like MongoDB, CouchDB, etc.
 * 
 * Strategy Pattern: Different serialization strategies for different document formats
 */
public interface DocumentSerializer<T> {
    
    /**
     * Serialize object to document format
     * @param item Item to serialize
     * @param parameters Additional serialization parameters
     * @return Document representation
     * @throws RuntimeException if serialization fails
     */
    Document serialize(T item, Map<String, Object> parameters);
    
    /**
     * Serialize multiple objects to document list
     * @param items Items to serialize
     * @param parameters Additional serialization parameters
     * @return List of document representations
     * @throws RuntimeException if serialization fails
     */
    List<Document> serialize(List<T> items, Map<String, Object> parameters);
    
    /**
     * Deserialize document to object
     * @param document Document to deserialize
     * @param itemClass Target class for deserialization
     * @return Deserialized object
     * @throws RuntimeException if deserialization fails
     */
    T deserialize(Document document, Class<T> itemClass);
    
    /**
     * Deserialize multiple documents to object list
     * @param documents Documents to deserialize
     * @param itemClass Target class for deserialization
     * @return List of deserialized objects
     * @throws RuntimeException if deserialization fails
     */
    List<T> deserialize(List<Document> documents, Class<T> itemClass);
    
    /**
     * Partial serialization with specific fields only
     * @param item Item to serialize
     * @param includedFields Fields to include in serialization
     * @param parameters Additional serialization parameters
     * @return Partial document representation
     * @throws RuntimeException if serialization fails
     */
    Document serializePartial(T item, List<String> includedFields, Map<String, Object> parameters);
    
    /**
     * Partial deserialization updating existing object
     * @param document Document with partial data
     * @param existingItem Existing item to update
     * @return Updated item
     * @throws RuntimeException if deserialization fails
     */
    T deserializePartial(Document document, T existingItem);
    
    /**
     * Serialize to JSON string
     * @param item Item to serialize
     * @param parameters Additional serialization parameters
     * @return JSON string representation
     * @throws RuntimeException if serialization fails
     */
    String serializeToJson(T item, Map<String, Object> parameters);
    
    /**
     * Deserialize from JSON string
     * @param json JSON string to deserialize
     * @param itemClass Target class for deserialization
     * @return Deserialized object
     * @throws RuntimeException if deserialization fails
     */
    T deserializeFromJson(String json, Class<T> itemClass);
    
    /**
     * Serialize to binary format
     * @param item Item to serialize
     * @param parameters Additional serialization parameters
     * @return Binary representation
     * @throws RuntimeException if serialization fails
     */
    byte[] serializeToBinary(T item, Map<String, Object> parameters);
    
    /**
     * Deserialize from binary format
     * @param data Binary data to deserialize
     * @param itemClass Target class for deserialization
     * @return Deserialized object
     * @throws RuntimeException if deserialization fails
     */
    T deserializeFromBinary(byte[] data, Class<T> itemClass);
    
    /**
     * Get field names that will be serialized
     * @param itemClass Item class
     * @return List of serializable field names
     */
    List<String> getSerializableFields(Class<T> itemClass);
    
    /**
     * Check if field should be serialized
     * @param fieldName Field name
     * @param itemClass Item class
     * @return true if field should be serialized
     */
    boolean shouldSerializeField(String fieldName, Class<T> itemClass);
    
    /**
     * Get serialization configuration
     * @return Map of configuration options
     */
    Map<String, Object> getSerializationConfig();
    
    /**
     * Configure serialization options
     * @param config Configuration options
     */
    void configureSerializer(Map<String, Object> config);
    
    /**
     * Get supported item classes
     * @return List of supported classes
     */
    List<Class<?>> getSupportedClasses();
    
    /**
     * Check if class is supported
     * @param itemClass Class to check
     * @return true if class is supported
     */
    boolean supportsClass(Class<?> itemClass);
    
    /**
     * Get serialization statistics
     * @return Map of serialization performance statistics
     */
    Map<String, Object> getSerializationStats();
    
    /**
     * Reset serialization statistics
     */
    void resetSerializationStats();
    
    /**
     * Clone/copy object through serialization
     * @param item Item to clone
     * @return Cloned item
     * @throws RuntimeException if cloning fails
     */
    T clone(T item);
    
    /**
     * Compare two objects for equality using serialization
     * @param item1 First item
     * @param item2 Second item
     * @return true if items are equal when serialized
     */
    boolean deepEquals(T item1, T item2);
}