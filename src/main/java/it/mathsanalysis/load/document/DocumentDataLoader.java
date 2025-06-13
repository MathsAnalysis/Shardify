package it.mathsanalysis.load.document;

import it.mathsanalysis.load.core.AbstractDataLoader;
import it.mathsanalysis.load.core.result.HealthStatus;
import it.mathsanalysis.load.spi.connection.DocumentConnectionProvider;
import it.mathsanalysis.load.spi.query.DocumentQueryBuilder;
import it.mathsanalysis.load.spi.serialization.DocumentSerializer;

import java.util.*;

/**
 * Document database implementation for NoSQL databases.
 *
 * Optimized for document databases like MongoDB, CouchDB, DynamoDB, etc.
 * Features:
 * - Schema-less document operations
 * - Flexible serialization strategies
 * - Index management
 * - Aggregation pipeline support
 * - Full-text search capabilities
 * - Geospatial query support
 *
 * Performance Optimizations:
 * - Bulk operations for batch inserts
 * - Connection pooling
 * - Document serialization caching
 * - Query result caching
 * - Lazy loading for large documents
 *
 * Thread Safety: This class is thread-safe and can be used concurrently
 * from multiple threads.
 *
 * @param <T> The type of items being loaded/saved
 * @param <ID> The type of item identifiers
 */
public abstract class DocumentDataLoader<T, ID> extends AbstractDataLoader<T, ID> {

    protected final DocumentConnectionProvider connectionProvider;
    protected final DocumentSerializer<T> serializer;
    protected final DocumentQueryBuilder<T> queryBuilder;
    protected final String collectionName;
    protected final String databaseName;

    /**
     * Constructor for document data loader
     *
     * @param itemType Class of items this loader handles
     * @param idType Class of item identifiers
     * @param connectionProvider Document database connection provider
     * @param serializer Document serializer for object conversion
     * @param queryBuilder Document query builder
     * @param collectionName Target collection name
     * @param databaseName Target database name
     * @param configuration Loader configuration
     */
    protected DocumentDataLoader(
            Class<T> itemType,
            Class<ID> idType,
            DocumentConnectionProvider connectionProvider,
            DocumentSerializer<T> serializer,
            DocumentQueryBuilder<T> queryBuilder,
            String collectionName,
            String databaseName,
            Map<String, Object> configuration) {

        super(itemType, idType, configuration);
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.serializer = Objects.requireNonNull(serializer, "Serializer cannot be null");
        this.queryBuilder = Objects.requireNonNull(queryBuilder, "Query builder cannot be null");
        this.collectionName = Objects.requireNonNull(collectionName, "Collection name cannot be null");
        this.databaseName = databaseName; // Can be null for some databases
    }

    @Override
    protected final T doSave(T item, Map<String, Object> parameters) {
        // Serialize item to document format
        var document = serializer.serialize(item, parameters);

        // Get collection and perform insert
        var collection = connectionProvider.getCollection(collectionName);
        var insertResult = collection.insertOne(document);

        // Deserialize back to get any generated fields (ID, timestamps, etc.)
        return serializer.deserialize(document, getItemType());
    }

    @Override
    protected final List<T> doSaveBatch(List<T> items, Map<String, Object> parameters) {
        if (items.isEmpty()) {
            return List.of();
        }

        // Serialize all items to documents
        var documents = serializer.serialize(items, parameters);

        // Get collection and perform bulk insert
        var collection = connectionProvider.getCollection(collectionName);
        var insertResult = collection.insertMany(documents);

        // Deserialize back to get generated fields
        return serializer.deserialize(documents, getItemType());
    }

    @Override
    protected final Optional<T> doFindById(ID id) {
        // Build find by ID query
        var query = queryBuilder.buildFindByIdQuery(id);

        // Execute query
        var collection = connectionProvider.getCollection(collectionName);
        var document = collection.findOne(query);

        if (document != null) {
            var item = serializer.deserialize(document, getItemType());
            return Optional.of(item);
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected final void doInitializeStorage(Map<String, Object> parameters) {
        var database = connectionProvider.getDatabase();

        // Create collection if it doesn't exist
        if (!database.collectionExists(collectionName)) {
            database.createCollection(collectionName, parameters);
        }

        // Create indexes if specified
        var indexes = (List<Map<String, Object>>) parameters.get("indexes");
        if (indexes != null) {
            for (var indexSpec : indexes) {
                var indexOptions = (Map<String, Object>) indexSpec.getOrDefault("options", Map.of());
                var indexQuery = queryBuilder.buildCreateIndexQuery(indexSpec, indexOptions);

                // Execute index creation
                database.createIndex(collectionName, (Map<String, Object>) indexQuery, indexOptions);
            }
        }
    }

    @Override
    protected final HealthStatus doHealthCheck() {
        try {
            // Test connection provider health
            if (!connectionProvider.isHealthy()) {
                return HealthStatus.unhealthy("Document connection provider is not healthy", getConnectionStats());
            }

            // Test database ping
            var database = connectionProvider.getDatabase();
            if (!database.ping()) {
                return HealthStatus.unhealthy("Database ping failed", getConnectionStats());
            }

            // Test collection access
            var collection = connectionProvider.getCollection(collectionName);
            var documentCount = collection.countDocuments();

            Map<String, Object> healthMetrics = new HashMap<>();
            healthMetrics.put("databaseName", databaseName != null ? databaseName : "unknown");
            healthMetrics.put("collectionName", collectionName);
            healthMetrics.put("documentCount", documentCount);
            healthMetrics.put("databaseType", queryBuilder.getDatabaseType());
            healthMetrics.put("lastHealthCheck", System.currentTimeMillis());

            return HealthStatus.healthy(healthMetrics, "Document database is healthy");

        } catch (Exception e) {
            Map<String, Object> healthMetrics = new HashMap<>();
            healthMetrics.put("databaseName", databaseName != null ? databaseName : "unknown");
            healthMetrics.put("collectionName", collectionName);
            healthMetrics.put("error", e.getMessage());
            healthMetrics.put("errorType", e.getClass().getSimpleName());
            healthMetrics.put("timestamp", System.currentTimeMillis());

            return HealthStatus.unhealthy("Health check failed: " + e.getMessage(), healthMetrics);
        }
    }

    @Override
    protected final Map<String, Object> getConnectionStats() {
        return connectionProvider.getConnectionStats();
    }

    // Additional document-specific methods

    /**
     * Find documents by custom criteria
     *
     * @param criteria Search criteria
     * @param parameters Additional query parameters
     * @return List of matching items
     */
    public List<T> findByCriteria(Map<String, Object> criteria, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        try {
            var collection = connectionProvider.getCollection(collectionName);
            var query = queryBuilder.buildFindQuery(criteria, parameters);
            var documents = collection.find(query);

            var results = serializer.deserialize(documents, getItemType());

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("findByCriteria", System.nanoTime() - startTime);
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to find documents by criteria in collection: " + collectionName, e);
        }
    }

    /**
     * Find documents with limit
     *
     * @param criteria Search criteria
     * @param limit Maximum number of documents
     * @param parameters Additional query parameters
     * @return List of matching items
     */
    public List<T> findByCriteria(Map<String, Object> criteria, int limit, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        try {
            var collection = connectionProvider.getCollection(collectionName);
            var query = queryBuilder.buildFindQuery(criteria, parameters);
            var documents = collection.find(query, limit);

            var results = serializer.deserialize(documents, getItemType());

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("findByCriteriaWithLimit", System.nanoTime() - startTime);
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to find documents by criteria with limit in collection: " + collectionName, e);
        }
    }

    /**
     * Count documents by criteria
     *
     * @param criteria Count criteria
     * @return Number of matching documents
     */
    public long countByCriteria(Map<String, Object> criteria) {
        var startTime = System.nanoTime();

        try {
            var collection = connectionProvider.getCollection(collectionName);
            var query = queryBuilder.buildCountQuery(criteria);
            var count = collection.countDocuments(query);

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("countByCriteria", System.nanoTime() - startTime);
            }

            return count;

        } catch (Exception e) {
            throw new RuntimeException("Failed to count documents by criteria in collection: " + collectionName, e);
        }
    }

    /**
     * Update document by ID
     *
     * @param item Item with updated values
     * @param parameters Additional parameters
     * @return Updated item
     */
    public T update(T item, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        try {
            var collection = connectionProvider.getCollection(collectionName);
            var updateQuery = queryBuilder.buildUpdateQuery(item, parameters);
            var document = serializer.serialize(item, parameters);

            var result = collection.replaceOne(updateQuery, document);

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("update", System.nanoTime() - startTime);
            }

            return item;

        } catch (Exception e) {
            throw new RuntimeException("Failed to update document in collection: " + collectionName, e);
        }
    }

    /**
     * Delete document by ID
     *
     * @param id ID of document to delete
     * @return true if document was deleted
     */
    public boolean deleteById(ID id) {
        if (id == null) {
            return false;
        }

        var startTime = System.nanoTime();

        try {
            var collection = connectionProvider.getCollection(collectionName);
            var query = queryBuilder.buildFindByIdQuery(id);
            var result = collection.deleteOne(query);

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("deleteById", System.nanoTime() - startTime);
            }

            return result != null; // Implementation-specific

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete document by ID in collection: " + collectionName, e);
        }
    }

    /**
     * Execute aggregation pipeline
     *
     * @param pipeline Aggregation pipeline stages
     * @param parameters Additional parameters
     * @return List of aggregation results
     */
    public List<T> aggregate(List<Map<String, Object>> pipeline, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        try {
            var collection = connectionProvider.getCollection(collectionName);
            var aggregationQuery = queryBuilder.buildAggregationQuery(pipeline, parameters);

            // Execute aggregation - implementation specific
            var documents = collection.aggregate(aggregationQuery);
            var results = serializer.deserialize(documents, getItemType());

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("aggregate", System.nanoTime() - startTime);
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute aggregation pipeline in collection: " + collectionName, e);
        }
    }

    /**
     * Text search in collection
     *
     * @param searchText Text to search for
     * @param parameters Additional search parameters
     * @return List of matching items
     */
    public List<T> textSearch(String searchText, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        try {
            var collection = connectionProvider.getCollection(collectionName);
            var searchQuery = queryBuilder.buildTextSearchQuery(searchText, parameters);
            var documents = collection.find(searchQuery);

            var results = serializer.deserialize(documents, getItemType());

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("textSearch", System.nanoTime() - startTime);
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute text search in collection: " + collectionName, e);
        }
    }

    /**
     * Get distinct values for a field
     *
     * @param fieldName Field name
     * @param criteria Optional criteria
     * @return List of distinct values
     */
    public List<Object> getDistinctValues(String fieldName, Map<String, Object> criteria) {
        var startTime = System.nanoTime();

        try {
            var collection = connectionProvider.getCollection(collectionName);
            var query = criteria != null ? queryBuilder.buildFindQuery(criteria, Map.of()) : Map.of();
            var distinctValues = collection.distinct(fieldName, query);

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("getDistinctValues", System.nanoTime() - startTime);
            }

            return distinctValues;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get distinct values for field: " + fieldName + " in collection: " + collectionName, e);
        }
    }

    /**
     * Create index on collection
     *
     * @param indexSpec Index specification
     * @param options Index options
     */
    public void createIndex(Map<String, Object> indexSpec, Map<String, Object> options) {
        try {
            var database = connectionProvider.getDatabase();
            database.createIndex(collectionName, indexSpec, options);

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("createIndex", 0);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to create index in collection: " + collectionName, e);
        }
    }

    /**
     * Geospatial query support
     *
     * @param location Geographic coordinates
     * @param parameters Query parameters (radius, units, etc.)
     * @return List of items within specified area
     */
    public List<T> findNear(Map<String, Object> location, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        try {
            var collection = connectionProvider.getCollection(collectionName);
            var geoQuery = queryBuilder.buildGeospatialQuery(location, parameters);
            var documents = collection.find(geoQuery);

            var results = serializer.deserialize(documents, getItemType());

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("findNear", System.nanoTime() - startTime);
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute geospatial query in collection: " + collectionName, e);
        }
    }

    /**
     * Regular expression search
     *
     * @param field Field to search
     * @param pattern Regex pattern
     * @param options Regex options
     * @return List of matching items
     */
    public List<T> findByRegex(String field, String pattern, Map<String, Object> options) {
        var startTime = System.nanoTime();

        try {
            var collection = connectionProvider.getCollection(collectionName);
            var regexQuery = queryBuilder.buildRegexQuery(field, pattern, options);
            var documents = collection.find(regexQuery);

            var results = serializer.deserialize(documents, getItemType());

            if (isMetricsEnabled()) {
                getMetrics().recordOperation("findByRegex", System.nanoTime() - startTime);
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute regex query in collection: " + collectionName, e);
        }
    }

    /**
     * Get debug query representation
     *
     * @param item The item to generate query for
     * @param parameters Additional parameters
     * @return The document query representation
     */
    public Object getDebugQuery(T item, Map<String, Object> parameters) {
        if (item == null) {
            return Map.of("error", "Cannot generate debug query for null item");
        }

        try {
            return serializer.serialize(item, parameters);
        } catch (Exception e) {
            return Map.of("error", "Error generating debug query: " + e.getMessage());
        }
    }

    // Getters for subclasses
    protected final String getCollectionName() { return collectionName; }
    protected final String getDatabaseName() { return databaseName; }
    public final DocumentConnectionProvider getConnectionProvider() { return connectionProvider; }
    protected final DocumentSerializer<T> getSerializer() { return serializer; }
    protected final DocumentQueryBuilder<T> getQueryBuilder() { return queryBuilder; }

    /**
     * Get serialization statistics
     *
     * @return Map containing serialization performance metrics
     */
    public Map<String, Object> getSerializationStats() {
        return serializer.getSerializationStats();
    }

    /**
     * Get query statistics
     *
     * @return Map containing query execution statistics
     */
    public Map<String, Object> getQueryStats() {
        return queryBuilder.getQueryStats();
    }

    /**
     * Reset all statistics
     */
    public void resetStats() {
        serializer.resetSerializationStats();
        queryBuilder.resetQueryStats();
        getMetrics().resetStats();
    }
}