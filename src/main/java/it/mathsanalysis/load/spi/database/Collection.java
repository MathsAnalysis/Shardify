package it.mathsanalysis.load.spi.database;
import it.mathsanalysis.load.spi.database.Document;

import java.util.List;
import java.util.Map;

/**
 * Enhanced collection interface for document databases
 * Supports advanced operations like aggregation, indexing, and bulk operations
 */
public interface Collection {

    // Basic operations
    Object insertOne(Document document);
    Object insertMany(List<Document> documents);
    Document findOne(Object query);
    List<Document> find(Object query);
    List<Document> find(Object query, int limit);
    long countDocuments();
    long countDocuments(Object query);

    // Update operations
    Object updateOne(Object filter, Object update);
    Object updateMany(Object filter, Object update);
    Object replaceOne(Object filter, Document replacement);

    // Delete operations
    Object deleteOne(Object query);
    Object deleteMany(Object query);

    // Advanced operations
    List<Document> aggregate(Object pipeline);
    List<Object> distinct(String fieldName, Object query);

    // Bulk operations
    Object bulkWrite(List<Object> operations, Map<String, Object> options);

    // Index operations
    void createIndex(Map<String, Object> indexSpec, Map<String, Object> options);
    void dropIndex(String indexName);
    List<Map<String, Object>> listIndexes();

    // Collection management
    Map<String, Object> getStats();
    void drop();

    // Advanced query operations
    Object watch(List<Map<String, Object>> pipeline, Map<String, Object> options);
    Object explainAggregate(List<Map<String, Object>> pipeline, Map<String, Object> options);
}