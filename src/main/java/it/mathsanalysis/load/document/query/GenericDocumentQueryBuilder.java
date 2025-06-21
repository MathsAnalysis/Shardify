package it.mathsanalysis.load.document.query;

import it.mathsanalysis.load.spi.query.DocumentQueryBuilder;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generic implementation of DocumentQueryBuilder for document databases
 * Provides a flexible way to build queries for various document types
 * Supports basic CRUD operations and query statistics
 *
 * @param <T> Type of the document item
 */
public class GenericDocumentQueryBuilder<T> implements DocumentQueryBuilder<T> {
    
    private final Class<T> itemType;
    private final String collectionName;
    private final String databaseType;
    private final Map<String, AtomicLong> queryStats = new ConcurrentHashMap<>();
    
    public GenericDocumentQueryBuilder(Class<T> itemType) {
        this.itemType = itemType;
        this.collectionName = itemType != null ? itemType.getSimpleName().toLowerCase() : "documents";
        this.databaseType = "generic";
        initializeStats();
    }
    
    public GenericDocumentQueryBuilder() {
        this.itemType = null; // Will be inferred at runtime
        this.collectionName = "documents";
        this.databaseType = "generic";
        initializeStats();
    }
    
    public GenericDocumentQueryBuilder(Class<T> itemType, String collectionName) {
        this.itemType = itemType;
        this.collectionName = collectionName != null ? collectionName : "documents";
        this.databaseType = "generic";
        initializeStats();
    }
    
    @Override
    public Object buildFindByIdQuery(Object id) {
        incrementQueryStat("findById");
        return Map.of("_id", id);
    }
    
    @Override
    public Object buildInsertQuery(T item, Map<String, Object> parameters) {
        incrementQueryStat("insert");
        // For generic implementation, assume item is already in document format
        return convertToDocument(item);
    }
    
    @Override
    public Object buildUpdateQuery(T item, Map<String, Object> parameters) {
        incrementQueryStat("update");
        var document = convertToDocument(item);
        var id = document.get("_id");
        if (id == null && itemType != null) {
            id = extractIdFromItem(item);
        }
        
        return Map.of(
            "filter", Map.of("_id", id),
            "update", Map.of("$set", document)
        );
    }
    
    @Override
    public Object buildDeleteQuery(T item, Map<String, Object> parameters) {
        incrementQueryStat("delete");
        var document = convertToDocument(item);
        var id = document.get("_id");
        if (id == null && itemType != null) {
            id = extractIdFromItem(item);
        }
        
        return Map.of("_id", id);
    }
    
    @Override
    public Object buildFindQuery(Map<String, Object> criteria, Map<String, Object> parameters) {
        incrementQueryStat("find");
        return criteria != null ? criteria : Map.of();
    }
    
    @Override
    public Object buildAggregationQuery(List<Map<String, Object>> pipeline, Map<String, Object> parameters) {
        incrementQueryStat("aggregation");
        return Map.of("pipeline", pipeline != null ? pipeline : List.of());
    }
    
    @Override
    public Object buildCountQuery(Map<String, Object> criteria) {
        incrementQueryStat("count");
        return Map.of("count", criteria != null ? criteria : Map.of());
    }
    
    @Override
    public Object buildDistinctQuery(String fieldName, Map<String, Object> criteria) {
        incrementQueryStat("distinct");
        return Map.of(
            "distinct", fieldName,
            "query", criteria != null ? criteria : Map.of()
        );
    }
    
    @Override
    public Object buildCreateIndexQuery(Map<String, Object> indexSpec, Map<String, Object> options) {
        incrementQueryStat("createIndex");
        return Map.of(
            "index", indexSpec != null ? indexSpec : Map.of(),
            "options", options != null ? options : Map.of()
        );
    }
    
    @Override
    public Object buildTextSearchQuery(String searchText, Map<String, Object> parameters) {
        incrementQueryStat("textSearch");
        return Map.of(
            "$text", Map.of("$search", searchText),
            "parameters", parameters != null ? parameters : Map.of()
        );
    }
    
    @Override
    public Object buildGeospatialQuery(Map<String, Object> location, Map<String, Object> parameters) {
        incrementQueryStat("geospatial");
        return Map.of(
            "location", location != null ? location : Map.of(),
            "parameters", parameters != null ? parameters : Map.of()
        );
    }
    
    @Override
    public Object buildRegexQuery(String field, String pattern, Map<String, Object> options) {
        incrementQueryStat("regex");
        return Map.of(
            field, Map.of("$regex", pattern),
            "options", options != null ? options : Map.of()
        );
    }
    
    @Override
    public String getCollectionName() {
        return collectionName;
    }
    
    @Override
    public String getDatabaseType() {
        return databaseType;
    }
    
    @Override
    public Map<String, Object> getQueryStats() {
        return queryStats.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                entry -> entry.getValue().get()
            ));
    }
    
    @Override
    public void resetQueryStats() {
        queryStats.values().forEach(counter -> counter.set(0));
    }
    
    private Map<String, Object> convertToDocument(T item) {
        if (item == null) {
            return Map.of();
        }
        
        if (item instanceof Map) {
            return (Map<String, Object>) item;
        }
        
        // For generic implementation, use reflection to convert object to map
        var document = new java.util.HashMap<String, Object>();
        
        try {
            var fields = item.getClass().getDeclaredFields();
            for (var field : fields) {
                field.setAccessible(true);
                var value = field.get(item);
                if (value != null) {
                    document.put(field.getName(), value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert item to document", e);
        }
        
        return document;
    }
    
    private Object extractIdFromItem(T item) {
        if (item == null) {
            return null;
        }
        
        try {
            // Try common ID field names
            String[] idFieldNames = {"id", "_id", "getId", "documentId"};
            
            for (String fieldName : idFieldNames) {
                try {
                    var field = item.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    var value = field.get(item);
                    if (value != null) {
                        return value;
                    }
                } catch (NoSuchFieldException exception) {
                    System.err.println("Failed to extract ID from item: " + exception.getMessage());
                }
            }
            
            // Try getter methods
            try {
                var method = item.getClass().getMethod("getId");
                return method.invoke(item);
            } catch (Exception exception) {
                System.err.println("Failed to extract ID from item: " + exception.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Failed to extract ID from item: " + e.getMessage());
        }
        
        return null;
    }
    
    private void initializeStats() {
        queryStats.put("findById", new AtomicLong(0));
        queryStats.put("insert", new AtomicLong(0));
        queryStats.put("update", new AtomicLong(0));
        queryStats.put("delete", new AtomicLong(0));
        queryStats.put("find", new AtomicLong(0));
        queryStats.put("aggregation", new AtomicLong(0));
        queryStats.put("count", new AtomicLong(0));
        queryStats.put("distinct", new AtomicLong(0));
        queryStats.put("createIndex", new AtomicLong(0));
        queryStats.put("textSearch", new AtomicLong(0));
        queryStats.put("geospatial", new AtomicLong(0));
        queryStats.put("regex", new AtomicLong(0));
    }
    
    private void incrementQueryStat(String operation) {
        queryStats.get(operation).incrementAndGet();
    }
}