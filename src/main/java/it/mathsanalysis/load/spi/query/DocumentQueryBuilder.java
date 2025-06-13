package it.mathsanalysis.load.spi.query;

import java.util.List;
import java.util.Map;

/**
 * Builder for document database queries
 * Handles document-specific query patterns and operations
 *
 * Strategy Pattern: Different implementations for different document databases
 */
public interface DocumentQueryBuilder<T> {

    /**
     * Build find by ID query for documents
     * @param id Document identifier
     * @return Document query object
     * @throws RuntimeException if query cannot be built
     */
    Object buildFindByIdQuery(Object id);

    /**
     * Build insert query for document
     * @param item Item to insert
     * @param parameters Additional parameters for query customization
     * @return Document insert specification
     * @throws RuntimeException if query cannot be built
     */
    Object buildInsertQuery(T item, Map<String, Object> parameters);

    /**
     * Build update query for document
     * @param item Item with updated values
     * @param parameters Additional parameters for query customization
     * @return Document update specification
     * @throws RuntimeException if query cannot be built
     */
    Object buildUpdateQuery(T item, Map<String, Object> parameters);

    /**
     * Build delete query for document
     * @param item Item to delete
     * @param parameters Additional parameters for query customization
     * @return Document delete specification
     * @throws RuntimeException if query cannot be built
     */
    Object buildDeleteQuery(T item, Map<String, Object> parameters);

    /**
     * Build find query with custom criteria
     * @param criteria Search criteria
     * @param parameters Additional parameters for query customization
     * @return Document find specification
     * @throws RuntimeException if query cannot be built
     */
    Object buildFindQuery(Map<String, Object> criteria, Map<String, Object> parameters);

    /**
     * Build aggregation pipeline
     * @param pipeline List of aggregation stages
     * @param parameters Additional parameters for pipeline customization
     * @return Aggregation pipeline specification
     * @throws RuntimeException if pipeline cannot be built
     */
    Object buildAggregationQuery(List<Map<String, Object>> pipeline, Map<String, Object> parameters);

    /**
     * Build count query
     * @param criteria Count criteria
     * @return Document count specification
     * @throws RuntimeException if query cannot be built
     */
    Object buildCountQuery(Map<String, Object> criteria);

    /**
     * Build distinct query
     * @param fieldName Field name for distinct values
     * @param criteria Optional criteria for filtering
     * @return Document distinct specification
     * @throws RuntimeException if query cannot be built
     */
    Object buildDistinctQuery(String fieldName, Map<String, Object> criteria);

    /**
     * Build index creation specification
     * @param indexSpec Index specification
     * @param options Index options
     * @return Index creation specification
     * @throws RuntimeException if specification cannot be built
     */
    Object buildCreateIndexQuery(Map<String, Object> indexSpec, Map<String, Object> options);

    /**
     * Build text search query
     * @param searchText Text to search for
     * @param parameters Additional search parameters
     * @return Text search specification
     * @throws RuntimeException if query cannot be built
     */
    Object buildTextSearchQuery(String searchText, Map<String, Object> parameters);

    /**
     * Build geospatial query
     * @param location Geographic location criteria
     * @param parameters Additional geospatial parameters
     * @return Geospatial query specification
     * @throws RuntimeException if query cannot be built
     */
    Object buildGeospatialQuery(Map<String, Object> location, Map<String, Object> parameters);

    /**
     * Build regex query
     * @param field Field name to search
     * @param pattern Regular expression pattern
     * @param options Regex options
     * @return Regex query specification
     * @throws RuntimeException if query cannot be built
     */
    Object buildRegexQuery(String field, String pattern, Map<String, Object> options);

    /**
     * Get collection name for this builder
     * @return Collection name
     */
    String getCollectionName();

    /**
     * Get database type information
     * @return Database type name
     */
    String getDatabaseType();

    /**
     * Get query statistics
     * @return Query usage statistics
     */
    Map<String, Object> getQueryStats();

    /**
     * Reset query statistics
     */
    void resetQueryStats();
}