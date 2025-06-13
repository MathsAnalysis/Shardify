package it.mathsanalysis.load.impl.mapping;

import it.mathsanalysis.load.annotations.impl.Column;
import it.mathsanalysis.load.annotations.impl.Id;
import it.mathsanalysis.load.annotations.impl.Transient;
import it.mathsanalysis.load.spi.database.ResultSet;
import it.mathsanalysis.load.spi.mapping.ItemMapper;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reflection-based item mapper for SQL result sets
 * Maps database results back to Java objects using reflection
 * Optimized for performance with field caching
 */
public final class ReflectionItemMapper<T> implements ItemMapper<T> {

    private final Class<T> itemType;
    private final List<Field> fields;
    private final Map<String, Field> fieldMap;
    private final boolean isRecord;
    private final Map<String, Object> mappingStats = new ConcurrentHashMap<>();

    // Static caches for performance optimization
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, String>> COLUMN_MAPPING_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, String> PRIMARY_KEY_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Set<String>> TRANSIENT_FIELDS_CACHE = new ConcurrentHashMap<>();

    public ReflectionItemMapper(Class<T> itemType) {
        this.itemType = Objects.requireNonNull(itemType, "itemType cannot be null");
        this.isRecord = itemType.isRecord();

        // Use cached fields instead of recalculating - FIXED: No method reference with generics
        this.fields = FIELD_CACHE.computeIfAbsent(itemType, this::calculateFieldsForClass);

        this.fieldMap = this.fields.stream()
                .collect(Collectors.toMap(Field::getName, Function.identity()));

        // Process annotations once and cache
        processAnnotations();
        initializeMappingStats();
    }

    /**
     * Calculate fields for a class - avoiding generic issues
     */
    private List<Field> calculateFieldsForClass(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .filter(field -> !field.isAnnotationPresent(Transient.class))
                .peek(field -> field.setAccessible(true))
                .collect(Collectors.toList());
    }

    /**
     * Process annotations and cache results
     */
    private void processAnnotations() {
        COLUMN_MAPPING_CACHE.computeIfAbsent(itemType, _ -> processColumnMappings());
        PRIMARY_KEY_CACHE.computeIfAbsent(itemType, _ -> processPrimaryKey());
        TRANSIENT_FIELDS_CACHE.computeIfAbsent(itemType, _ -> processTransientFields());
    }

    /**
     * Process column mappings from @Column annotations
     */
    private Map<String, String> processColumnMappings() {
        Map<String, String> mappings = new HashMap<>();
        for (Field field : fields) {
            String columnName = field.getName(); // default

            if (field.isAnnotationPresent(Column.class)) {
                var column = field.getAnnotation(Column.class);
                if (!Objects.requireNonNull(column).name().isEmpty()) {
                    columnName = column.name();
                }
            }
            mappings.put(field.getName(), columnName);
        }
        return mappings;
    }

    /**
     * Process primary key from @Id annotation
     */
    private String processPrimaryKey() {
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                return field.getName();
            }
        }
        return "id"; // default
    }

    /**
     * Process transient fields from @Transient annotation
     */
    private Set<String> processTransientFields() {
        return fields.stream()
                .filter(field -> field.isAnnotationPresent(Transient.class))
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public T mapFromResultSet(ResultSet resultSet) {
        var startTime = System.nanoTime();

        try {
            T result;
            if (isRecord) {
                result = mapRecordFromResultSet(resultSet);
            } else {
                result = mapClassFromResultSet(resultSet);
            }

            recordMappingOperation(System.nanoTime() - startTime);
            return result;

        } catch (Exception e) {
            recordMappingOperation(System.nanoTime() - startTime);
            throw new RuntimeException("Failed to map result set to " + itemType.getSimpleName(), e);
        }
    }

    @Override
    public T mapFromResultSet(ResultSet resultSet, T existingItem) {
        if (isRecord) {
            // Records are immutable, so we create a new instance
            return mapFromResultSet(resultSet);
        } else {
            // For regular classes, we can update the existing instance
            return updateExistingItem(resultSet, existingItem);
        }
    }

    @Override
    public List<T> mapBatchFromResultSet(ResultSet resultSet, List<T> originalItems) {
        var mappedItems = new ArrayList<T>();
        var index = 0;

        while (resultSet.next() && index < originalItems.size()) {
            var mappedItem = mapFromResultSet(resultSet, originalItems.get(index));
            mappedItems.add(mappedItem);
            index++;
        }

        return mappedItems;
    }

    @Override
    public List<Object> extractParameters(T item) {
        var transientFields = TRANSIENT_FIELDS_CACHE.get(itemType);
        return fields.stream()
                .filter(field -> !transientFields.contains(field.getName()))
                .map(field -> getFieldValue(field, item))
                .toList();
    }

    @Override
    public Map<String, Object> extractParameterMap(T item) {
        return fields.stream()
                .collect(Collectors.toMap(
                        Field::getName,
                        field -> getFieldValue(field, item)
                ));
    }

    @Override
    public List<Object> extractParameters(T item, List<String> fieldNames) {
        return fieldNames.stream()
                .map(fieldName -> {
                    var field = fieldMap.get(fieldName);
                    if (field == null) {
                        throw new RuntimeException("Field not found: " + fieldName);
                    }
                    return getFieldValue(field, item);
                })
                .toList();
    }

    @Override
    public List<String> getFieldNames() {
        return fields.stream().map(Field::getName).toList();
    }

    @Override
    public String getPrimaryKeyField() {
        return PRIMARY_KEY_CACHE.get(itemType);
    }

    @Override
    public Class<T> getItemClass() {
        return itemType;
    }

    @Override
    public boolean isFieldNullable(String fieldName) {
        var field = fieldMap.get(fieldName);
        return field != null && !field.getType().isPrimitive();
    }

    @Override
    public Class<?> getFieldType(String fieldName) {
        var field = fieldMap.get(fieldName);
        return field != null ? field.getType() : Object.class;
    }

    @Override
    public Object getPrimaryKeyValue(T item) {
        var primaryKeyField = fieldMap.get(getPrimaryKeyField());
        return primaryKeyField != null ? getFieldValue(primaryKeyField, item) : null;
    }

    @Override
    public T setPrimaryKeyValue(T item, Object primaryKeyValue) {
        if (isRecord) {
            throw new UnsupportedOperationException("Cannot modify record primary key");
        } else {
            var primaryKeyField = fieldMap.get(getPrimaryKeyField());
            if (primaryKeyField != null) {
                setFieldValue(primaryKeyField, item, primaryKeyValue);
            }
            return item;
        }
    }

    @Override
    public T createNewInstance() {
        try {
            return itemType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create new instance of " + itemType.getSimpleName(), e);
        }
    }

    @Override
    public T createNewInstance(Map<String, Object> values) {
        if (isRecord) {
            return createRecordInstance(values);
        } else {
            var instance = createNewInstance();
            values.forEach((fieldName, value) -> {
                var field = fieldMap.get(fieldName);
                if (field != null) {
                    setFieldValue(field, instance, value);
                }
            });
            return instance;
        }
    }

    @Override
    public boolean hasGeneratedKey(T item) {
        var primaryKeyValue = getPrimaryKeyValue(item);
        return primaryKeyValue != null;
    }

    @Override
    public List<String> validateItem(T item) {
        var errors = new ArrayList<String>();

        fields.forEach(field -> {
            var value = getFieldValue(field, item);

            if (value == null && field.getType().isPrimitive()) {
                errors.add("Field " + field.getName() + " cannot be null");
            }
        });

        return errors;
    }

    @Override
    public Map<String, Object> getMappingStats() {
        return Map.copyOf(mappingStats);
    }

    @Override
    public void resetMappingStats() {
        mappingStats.clear();
        initializeMappingStats();
    }

    // Helper methods
    private T mapRecordFromResultSet(ResultSet resultSet) {
        var recordComponents = itemType.getRecordComponents();
        var values = Arrays.stream(recordComponents)
                .map(component -> {
                    var value = resultSet.getValue(component.getName());
                    return convertValue(value, component.getType());
                })
                .toArray();

        try {
            var constructor = Arrays.stream(itemType.getConstructors())
                    .filter(c -> c.getParameterCount() == recordComponents.length)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No suitable constructor found for record"));

            return (T) constructor.newInstance(values);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create record instance", e);
        }
    }

    private T mapClassFromResultSet(ResultSet resultSet) {
        try {
            var instance = itemType.getDeclaredConstructor().newInstance();

            fields.forEach(field -> {
                var value = resultSet.getValue(field.getName());
                var convertedValue = convertValue(value, field.getType());
                setFieldValue(field, instance, convertedValue);
            });

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create class instance", e);
        }
    }

    private T updateExistingItem(ResultSet resultSet, T existingItem) {
        fields.forEach(field -> {
            var value = resultSet.getValue(field.getName());
            if (value != null) {
                var convertedValue = convertValue(value, field.getType());
                setFieldValue(field, existingItem, convertedValue);
            }
        });

        return existingItem;
    }

    private T createRecordInstance(Map<String, Object> values) {
        var recordComponents = itemType.getRecordComponents();
        var constructorArgs = Arrays.stream(recordComponents)
                .map(component -> {
                    var value = values.get(component.getName());
                    return convertValue(value, component.getType());
                })
                .toArray();

        try {
            var constructor = Arrays.stream(itemType.getConstructors())
                    .filter(c -> c.getParameterCount() == recordComponents.length)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No suitable constructor found for record"));

            return (T) constructor.newInstance(constructorArgs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create record instance", e);
        }
    }

    private Object getFieldValue(Field field, T item) {
        try {
            field.setAccessible(true);
            return field.get(item);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get field value: " + field.getName(), e);
        }
    }

    private void setFieldValue(Field field, T item, Object value) {
        try {
            field.setAccessible(true);
            field.set(item, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field value: " + field.getName(), e);
        }
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;

        return switch (targetType.getSimpleName()) {
            case "String" -> value.toString();
            case "Integer", "int" -> Integer.valueOf(value.toString());
            case "Long", "long" -> Long.valueOf(value.toString());
            case "Boolean", "boolean" -> Boolean.valueOf(value.toString());
            case "Double", "double" -> Double.valueOf(value.toString());
            case "Float", "float" -> Float.valueOf(value.toString());
            default -> value;
        };
    }

    private void recordMappingOperation(long durationNanos) {
        mappingStats.merge("mapFromResultSet_count", 1L, (a, b) -> (Long) a + (Long) b);
        mappingStats.merge("mapFromResultSet_total_time", durationNanos, (a, b) -> (Long) a + (Long) b);
    }

    private void initializeMappingStats() {
        mappingStats.put("mapFromResultSet_count", 0L);
        mappingStats.put("mapFromResultSet_total_time", 0L);
        mappingStats.put("created_at", System.currentTimeMillis());
    }
}