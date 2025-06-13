package it.mathsanalysis.load.impl.database;

import it.mathsanalysis.load.spi.database.Document;

import java.util.*;

/**
 * Complete HashMap-based document implementation
 * High-performance document representation with full SPI compliance
 * <p>
 * Features:
 * - Full Document interface implementation
 * - Nested document support with dot notation
 * - JSON serialization support
 * - Deep copy and merge operations
 * - Type-safe value retrieval
 * - Metadata support
 * <p>
 * Performance optimizations:
 * - Uses HashMap for O(1) key access
 * - Lazy JSON serialization
 * - Efficient nested path traversal
 * - Minimal object allocations
 */
public final class HashMapDocument implements Document {

    private final Map<String, Object> data;
    private Map<String, Object> metadata;

    /**
     * Create empty document
     */
    public HashMapDocument() {
        this.data = new HashMap<>();
    }

    /**
     * Create document with initial data
     *
     * @param data Initial data map (copied defensively)
     */
    public HashMapDocument(Map<String, Object> data) {
        this.data = new HashMap<>(data != null ? data : Map.of());
    }

    @Override
    public Object get(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return data.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");

        var value = data.get(key);
        if (value == null) {
            return null;
        }

        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        // Attempt type conversion
        return convertValue(value, type);
    }

    @Override
    public Object getOrDefault(String key, Object defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        return data.getOrDefault(key, defaultValue);
    }

    @Override
    public Optional<Object> getOptional(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return Optional.ofNullable(data.get(key));
    }

    @Override
    public Object put(String key, Object value) {
        Objects.requireNonNull(key, "Key cannot be null");
        return data.put(key, value);
    }

    @Override
    public void putAll(Map<String, Object> map) {
        if (map != null) {
            data.putAll(map);
        }
    }

    @Override
    public Object remove(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return data.remove(key);
    }

    @Override
    public boolean containsKey(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Set<String> keySet() {
        return new HashSet<>(data.keySet()); // Defensive copy
    }

    @Override
    public Collection<Object> values() {
        return new ArrayList<>(data.values()); // Defensive copy
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return new HashSet<>(data.entrySet()); // Defensive copy
    }

    @Override
    public Map<String, Object> asMap() {
        return new HashMap<>(data); // Defensive copy
    }

    @Override
    public Map<String, Object> asMapDeepCopy() {
        return deepCopyMap(data);
    }

    @Override
    public void clear() {
        data.clear();
        if (metadata != null) {
            metadata.clear();
        }
    }

    @Override
    public Document copy() {
        var copy = new HashMapDocument(asMapDeepCopy());
        if (metadata != null) {
            copy.setMetadata(deepCopyMap(metadata));
        }
        return copy;
    }

    @Override
    public Document merge(Document other, boolean overwrite) {
        Objects.requireNonNull(other, "Other document cannot be null");

        var otherMap = other.asMap();
        for (var entry : otherMap.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();

            if (overwrite || !data.containsKey(key)) {
                data.put(key, value);
            }
        }

        return this;
    }

    @Override
    public Object getNestedValue(String path) {
        Objects.requireNonNull(path, "Path cannot be null");

        if (path.isEmpty()) {
            return null;
        }

        var parts = path.split("\\.");
        Object current = data;

        for (var part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null; // Path doesn't exist
            }
        }

        return current;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Document setNestedValue(String path, Object value) {
        Objects.requireNonNull(path, "Path cannot be null");

        if (path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        var parts = path.split("\\.");
        Map<String, Object> current = data;

        // Navigate to the parent of the target
        for (int i = 0; i < parts.length - 1; i++) {
            var part = parts[i];
            var next = current.get(part);

            if (next instanceof Map<?, ?> map) {
                current = (Map<String, Object>) map;
            } else {
                // Create new nested map
                var newMap = new HashMap<String, Object>();
                current.put(part, newMap);
                current = newMap;
            }
        }

        // Set the final value
        current.put(parts[parts.length - 1], value);
        return this;
    }

    @Override
    public boolean hasNestedPath(String path) {
        return getNestedValue(path) != null;
    }

    @Override
    public String toJson() {
        return jsonSerialize(data, false);
    }

    @Override
    public String toPrettyJson() {
        return jsonSerialize(data, true);
    }

    @Override
    public Object getId() {
        return data.get("_id");
    }

    @Override
    public Document setId(Object id) {
        data.put("_id", id);
        return this;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    @Override
    public Document setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : null;
        return this;
    }

    // Standard Object methods

    @Override
    public String toString() {
        return "HashMapDocument" + data.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HashMapDocument other)) return false;

        return Objects.equals(data, other.data) &&
                Objects.equals(metadata, other.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, metadata);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }

        try {
            if (type == String.class) {
                return (T) value.toString();
            } else if (type == Integer.class || type == int.class) {
                if (value instanceof Number number) {
                    return (T) Integer.valueOf(number.intValue());
                }
                return (T) Integer.valueOf(value.toString());
            } else if (type == Long.class || type == long.class) {
                if (value instanceof Number number) {
                    return (T) Long.valueOf(number.longValue());
                }
                return (T) Long.valueOf(value.toString());
            } else if (type == Double.class || type == double.class) {
                if (value instanceof Number number) {
                    return (T) Double.valueOf(number.doubleValue());
                }
                return (T) Double.valueOf(value.toString());
            } else if (type == Float.class || type == float.class) {
                if (value instanceof Number number) {
                    return (T) Float.valueOf(number.floatValue());
                }
                return (T) Float.valueOf(value.toString());
            } else if (type == Boolean.class || type == boolean.class) {
                if (value instanceof Boolean bool) {
                    return (T) bool;
                }
                return (T) Boolean.valueOf(value.toString());
            } else if (type == List.class && value instanceof Collection) {
                return (T) new ArrayList<>((Collection<?>) value);
            } else if (type == Map.class && value instanceof Map) {
                return (T) new HashMap<>((Map<?, ?>) value);
            } else {
                // Last resort - attempt direct cast
                return type.cast(value);
            }
        } catch (Exception e) {
            throw new ClassCastException("Cannot convert value '" + value +
                    "' of type " + value.getClass().getSimpleName() +
                    " to " + type.getSimpleName() + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopyMap(Map<String, Object> original) {
        var copy = new HashMap<String, Object>();

        for (var entry : original.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();

            switch (value) {
                case Map<?, ?> mapValue -> copy.put(key, deepCopyMap((Map<String, Object>) mapValue));
                case List<?> listValue -> copy.put(key, deepCopyList(listValue));
                case Set<?> setValue -> copy.put(key, new HashSet<>(setValue));
                case null, default -> // For null or other immutable types, direct assignment is fine
                        copy.put(key, value);
            }
        }

        return copy;
    }

    @SuppressWarnings("unchecked")
    private List<Object> deepCopyList(List<?> original) {
        var copy = new ArrayList<Object>();

        for (var item : original) {
            switch (item) {
                case Map<?, ?> mapItem -> copy.add(deepCopyMap((Map<String, Object>) mapItem));
                case List<?> listItem -> copy.add(deepCopyList(listItem));
                case Set<?> setItem -> copy.add(new HashSet<>(setItem));
                case null, default -> copy.add(item);
            }
        }

        return copy;
    }

    private String jsonSerialize(Object obj, boolean pretty) {
        return jsonSerializeInternal(obj, pretty, 0);
    }

    private String jsonSerializeInternal(Object obj, boolean pretty, int indentLevel) {
        switch (obj) {
            case null -> {
                return "null";
            }
            case String str -> {
                return "\"" + escapeJsonString(str) + "\"";
            }
            case Number number -> {
                return obj.toString();
            }
            case Boolean b -> {
                return obj.toString();
            }
            case Map<?, ?> map -> {
                var sb = new StringBuilder();
                sb.append("{");

                if (pretty && !map.isEmpty()) {
                    sb.append("\n");
                }

                var entries = map.entrySet();
                var iterator = entries.iterator();

                while (iterator.hasNext()) {
                    var entry = iterator.next();

                    if (pretty) {
                        sb.append("  ".repeat(indentLevel + 1));
                    }

                    sb.append("\"").append(escapeJsonString(entry.getKey().toString())).append("\":");

                    if (pretty) {
                        sb.append(" ");
                    }

                    sb.append(jsonSerializeInternal(entry.getValue(), pretty, indentLevel + 1));

                    if (iterator.hasNext()) {
                        sb.append(",");
                    }

                    if (pretty) {
                        sb.append("\n");
                    }
                }

                if (pretty && !map.isEmpty()) {
                    sb.append("  ".repeat(indentLevel));
                }

                sb.append("}");
                return sb.toString();
            }
            case Collection<?> collection -> {
                var sb = new StringBuilder();
                sb.append("[");

                if (pretty && !collection.isEmpty()) {
                    sb.append("\n");
                }

                var iterator = collection.iterator();

                while (iterator.hasNext()) {
                    var item = iterator.next();

                    if (pretty) {
                        sb.append("  ".repeat(indentLevel + 1));
                    }

                    sb.append(jsonSerializeInternal(item, pretty, indentLevel + 1));

                    if (iterator.hasNext()) {
                        sb.append(",");
                    }

                    if (pretty) {
                        sb.append("\n");
                    }
                }

                if (pretty && !collection.isEmpty()) {
                    sb.append("  ".repeat(indentLevel));
                }

                sb.append("]");
                return sb.toString();
            }
            default -> {
            }
        }

        // Fallback for other types
        return "\"" + escapeJsonString(obj.toString()) + "\"";
    }

    private String escapeJsonString(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    // Static factory methods for convenience

    /**
     * Create document from key-value pairs
     *
     * @param entries Key-value pairs (must be even number)
     * @return New document with specified entries
     */
    public static HashMapDocument of(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Entries must be key-value pairs (even number of arguments)");
        }

        var data = new HashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) {
            var key = entries[i].toString();
            var value = entries[i + 1];
            data.put(key, value);
        }

        return new HashMapDocument(data);
    }

    /**
     * Create empty document
     *
     * @return New empty document
     */
    public static HashMapDocument empty() {
        return new HashMapDocument();
    }

    /**
     * Create document from existing map
     *
     * @param map Source map
     * @return New document with copied data
     */
    public static HashMapDocument from(Map<String, Object> map) {
        return new HashMapDocument(map);
    }
}