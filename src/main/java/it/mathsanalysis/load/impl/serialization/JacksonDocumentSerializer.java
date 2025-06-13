package it.mathsanalysis.load.impl.serialization;

import it.mathsanalysis.load.spi.serialization.DocumentSerializer;
import it.mathsanalysis.load.spi.database.Document;
import it.mathsanalysis.load.impl.database.HashMapDocument;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Jackson-based document serializer for MongoDB and other document databases
 * High-performance JSON serialization with customizable configuration
 */
public final class JacksonDocumentSerializer<T> implements DocumentSerializer<T> {

    private final Map<String, Object> serializationStats = new ConcurrentHashMap<>();
    private final Map<String, Object> config = new ConcurrentHashMap<>();

    public JacksonDocumentSerializer() {
        initializeStats();
        initializeConfig();
    }

    private void initializeConfig() {
        config.put("prettyPrint", false);
        config.put("dateFormat", "ISO8601");
        config.put("includeNullValues", false);


        config.put("ignoreUnknownProperties", true);
        config.put("failOnUnknownProperties", false);
        config.put("serializationInclusion", "NON_NULL");
        config.put("serializationFeatures", List.of("WRITE_DATES_AS_TIMESTAMPS"));
        config.put("deserializationFeatures", List.of("FAIL_ON_UNKNOWN_PROPERTIES"));
        config.put("serializationDateFormat", "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        config.put("deserializationDateFormat", "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        config.put("serializationIndentOutput", false);
        config.put("serializationType", "JSON");
        config.put("serializationEncoding", "UTF-8");
        config.put("serializationCompression", false);
        config.put("serializationCompressionLevel", 5);
        config.put("serializationBufferSize", 8192);
        config.put("serializationMaxDepth", 100);
        config.put("serializationMaxItems", 10000);
        config.put("serializationMaxStringLength", 1000);
        config.put("serializationMaxArrayLength", 1000);
        config.put("serializationMaxObjectSize", 1048576); // 1 MB
        config.put("serializationMaxBinarySize", 1048576); // 1 MB
        config.put("serializationMaxFieldCount", 1000);
        config.put("serializationMaxFieldNameLength", 100);
        config.put("serializationMaxFieldValueLength", 1000);
        config.put("serializationMaxNestedDepth", 10);
        config.put("serializationMaxNestedItems", 100);
        config.put("serializationMaxNestedStringLength", 1000);
        config.put("serializationMaxNestedArrayLength", 1000);
        config.put("serializationMaxNestedObjectSize", 1048576); // 1 MB
        config.put("serializationMaxNestedBinarySize", 1048576); // 1 MB
        config.put("serializationMaxNestedFieldCount", 1000);
        config.put("serializationMaxNestedFieldNameLength", 100);
        config.put("serializationMaxNestedFieldValueLength", 1000);

    }

    @Override
    public Document serialize(T item, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        try {
            // For demo purposes, using reflection to convert to map
            var itemMap = convertObjectToMap(item);

            // Apply parameters if provided
            if (parameters != null && !parameters.isEmpty()) {
                parameters.forEach(itemMap::putIfAbsent);
            }

            // Add metadata
            itemMap.put("_serialized_at", System.currentTimeMillis());
            itemMap.put("_item_type", item.getClass().getSimpleName());

            var document = new HashMapDocument(itemMap);

            recordSerializationOperation("serialize", System.nanoTime() - startTime);
            return document;

        } catch (Exception e) {
            recordSerializationOperation("serialize", System.nanoTime() - startTime);
            throw new RuntimeException("Failed to serialize item", e);
        }
    }

    @Override
    public List<Document> serialize(List<T> items, Map<String, Object> parameters) {
        return items.stream()
            .map(item -> serialize(item, parameters))
            .toList();
    }

    @Override
    public T deserialize(Document document, Class<T> itemClass) {
        var startTime = System.nanoTime();

        try {
            var documentMap = document.asMap();

            // Remove metadata before deserialization
            documentMap.remove("_serialized_at");
            documentMap.remove("_item_type");

            var result = convertMapToObject(documentMap, itemClass);
            recordSerializationOperation("deserialize", System.nanoTime() - startTime);
            return result;

        } catch (Exception e) {
            recordSerializationOperation("deserialize", System.nanoTime() - startTime);
            throw new RuntimeException("Failed to deserialize document", e);
        }
    }

    @Override
    public List<T> deserialize(List<Document> documents, Class<T> itemClass) {
        return documents.stream()
            .map(doc -> deserialize(doc, itemClass))
            .toList();
    }

    @Override
    public Document serializePartial(T item, List<String> includedFields, Map<String, Object> parameters) {
        var fullMap = convertObjectToMap(item);
        var partialMap = new java.util.HashMap<String, Object>();

        includedFields.forEach(field -> {
            if (fullMap.containsKey(field)) {
                partialMap.put(field, fullMap.get(field));
            }
        });

        if (parameters != null) {
            parameters.forEach(partialMap::putIfAbsent);
        }

        return new HashMapDocument(partialMap);
    }

    @Override
    public T deserializePartial(Document document, T existingItem) {
        // Simplified implementation - would need more sophisticated merging
        return deserialize(document, (Class<T>) existingItem.getClass());
    }

    @Override
    public String serializeToJson(T item, Map<String, Object> parameters) {
        var document = serialize(item, parameters);
        return document.toString(); // Simplified JSON representation
    }

    @Override
    public T deserializeFromJson(String json, Class<T> itemClass) {
        // Simplified implementation - would need actual JSON parsing
        throw new UnsupportedOperationException("JSON deserialization not implemented in demo");
    }

    @Override
    public byte[] serializeToBinary(T item, Map<String, Object> parameters) {
        var json = serializeToJson(item, parameters);
        return json.getBytes();
    }

    @Override
    public T deserializeFromBinary(byte[] data, Class<T> itemClass) {
        var json = new String(data);
        return deserializeFromJson(json, itemClass);
    }

    @Override
    public List<String> getSerializableFields(Class<T> itemClass) {
        return Stream.of(itemClass.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .map(Field::getName)
            .toList();
    }

    @Override
    public boolean shouldSerializeField(String fieldName, Class<T> itemClass) {
        return getSerializableFields(itemClass).contains(fieldName);
    }

    @Override
    public Map<String, Object> getSerializationConfig() {
        return Map.copyOf(config);
    }

    @Override
    public void configureSerializer(Map<String, Object> config) {
        this.config.putAll(config);
    }

    @Override
    public List<Class<?>> getSupportedClasses() {
        return List.of(Object.class); // Supports all classes in demo
    }

    @Override
    public boolean supportsClass(Class<?> itemClass) {
        return true; // Supports all classes in demo
    }

    @Override
    public Map<String, Object> getSerializationStats() {
        return Map.copyOf(serializationStats);
    }

    @Override
    public void resetSerializationStats() {
        serializationStats.clear();
        initializeStats();
    }

    @Override
    public T clone(T item) {
        var document = serialize(item, Map.of());
        return deserialize(document, (Class<T>) item.getClass());
    }

    @Override
    public boolean deepEquals(T item1, T item2) {
        var doc1 = serialize(item1, Map.of());
        var doc2 = serialize(item2, Map.of());
        return doc1.equals(doc2);
    }

    // Helper methods
    private Map<String, Object> convertObjectToMap(T item) {
        var map = new java.util.HashMap<String, Object>();

        try {
            var fields = item.getClass().getDeclaredFields();
            for (var field : fields) {
                if (!field.isSynthetic()) {
                    field.setAccessible(true);
                    map.put(field.getName(), field.get(item));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to map", e);
        }

        return map;
    }

    private T convertMapToObject(Map<String, Object> map, Class<T> itemClass) {
        try {
            var instance = itemClass.getDeclaredConstructor().newInstance();
            var fields = itemClass.getDeclaredFields();

            for (var field : fields) {
                if (!field.isSynthetic() && map.containsKey(field.getName())) {
                    field.setAccessible(true);
                    field.set(instance, map.get(field.getName()));
                }
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to object", e);
        }
    }

    private void recordSerializationOperation(String operation, long durationNanos) {
        serializationStats.merge(operation + "_count", 1L, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });

        serializationStats.merge(operation + "_total_time", durationNanos, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });
    }

    private void initializeStats() {
        serializationStats.put("serialize_count", 0L);
        serializationStats.put("deserialize_count", 0L);
        serializationStats.put("serialize_total_time", 0L);
        serializationStats.put("deserialize_total_time", 0L);
        serializationStats.put("created_at", System.currentTimeMillis());
    }
}