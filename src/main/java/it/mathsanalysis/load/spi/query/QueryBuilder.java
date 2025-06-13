package it.mathsanalysis.load.spi.query;

import it.mathsanalysis.load.spi.query.model.Query;
import it.mathsanalysis.load.spi.query.model.BatchQuery;

import java.util.List;
import java.util.Map;

/**
 * Enhanced SQL query builder interface
 * Supports advanced SQL operations and optimizations
 */
public interface QueryBuilder<T> {

    // Basic CRUD operations
    Query buildInsertQuery(T item, Map<String, Object> parameters);
    BatchQuery buildBatchInsertQuery(List<T> items, Map<String, Object> parameters);
    Query buildUpdateQuery(T item, Map<String, Object> parameters);
    Query buildDeleteQuery(T item, Map<String, Object> parameters);
    Query buildSelectByIdQuery(Object id);
    Query buildSelectQuery(Map<String, Object> criteria, Map<String, Object> parameters);

    // DDL operations
    Query buildCreateTableQuery(Map<String, Object> parameters);
    Query buildDropTableQuery(Map<String, Object> parameters);
    Query buildCreateIndexQuery(Map<String, Object> parameters);
    Query buildDropIndexQuery(String indexName);

    // Advanced queries
    Query buildCountQuery(Map<String, Object> criteria);
    Query buildExistsQuery(Map<String, Object> criteria);
    Query buildPaginatedQuery(Map<String, Object> criteria, int offset, int limit);
    Query buildOrderedQuery(Map<String, Object> criteria, List<String> orderBy);

    // Aggregation queries
    Query buildSumQuery(String column, Map<String, Object> criteria);
    Query buildAverageQuery(String column, Map<String, Object> criteria);
    Query buildMinMaxQuery(String column, Map<String, Object> criteria, boolean isMin);
    Query buildGroupByQuery(List<String> groupBy, Map<String, Object> criteria);

    // Join operations
    Query buildJoinQuery(String joinTable, String joinCondition, Map<String, Object> criteria);
    Query buildLeftJoinQuery(String joinTable, String joinCondition, Map<String, Object> criteria);

    // Bulk operations
    BatchQuery buildBatchUpdateQuery(List<T> items, Map<String, Object> parameters);
    BatchQuery buildBatchDeleteQuery(List<T> items, Map<String, Object> parameters);

    // Utility methods
    Map<String, Integer> getCacheStats();
    void clearCache();
    String getTableName();
    String getSqlDialect();
    void setCachingEnabled(boolean enabled);
    boolean isCachingEnabled();

    // Query optimization
    Query addQueryHint(Query query, String hint);
    Query optimizeQuery(Query query, Map<String, Object> optimizationHints);

    // Schema operations
    Query buildAlterTableQuery(Map<String, Object> alterations);
    Query buildCreateConstraintQuery(Map<String, Object> constraint);
    Query buildDropConstraintQuery(String constraintName);

    // Performance monitoring
    Map<String, Object> getQueryPerformanceStats();
    void resetQueryPerformanceStats();
}
