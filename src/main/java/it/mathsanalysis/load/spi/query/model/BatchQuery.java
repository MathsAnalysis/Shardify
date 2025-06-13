package it.mathsanalysis.load.spi.query.model;

import java.util.List;
import java.util.Objects;

/**
 * Batch query container for multiple operations
 * Optimized for batch insert/update operations
 * 
 * Value Object Pattern: Immutable data container for batch SQL operations
 */
public record BatchQuery(
    String sql,
    List<List<Object>> parameterSets,
    boolean returnGeneratedKeys,
    Query.QueryType type,
    int batchSize
) {
    
    /**
     * Compact constructor with validation
     */
    public BatchQuery {
        Objects.requireNonNull(sql, "SQL cannot be null");
        Objects.requireNonNull(parameterSets, "Parameter sets cannot be null");
        Objects.requireNonNull(type, "Query type cannot be null");
        
        if (sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL cannot be empty");
        }
        
        if (batchSize < 0) {
            throw new IllegalArgumentException("Batch size cannot be negative");
        }
        
        // Make parameter sets immutable
        parameterSets = parameterSets.stream()
            .map(List::copyOf)
            .toList();
    }
    
    /**
     * Create batch query with inferred batch size
     * @param sql SQL statement
     * @param parameterSets List of parameter sets
     * @param returnGeneratedKeys Whether to return generated keys
     * @return BatchQuery instance
     */
    public static BatchQuery of(String sql, List<List<Object>> parameterSets, boolean returnGeneratedKeys) {
        return new BatchQuery(
            sql, 
            parameterSets, 
            returnGeneratedKeys, 
            inferQueryType(sql),
            parameterSets.size()
        );
    }
    
    /**
     * Create batch INSERT query
     * @param sql INSERT statement
     * @param parameterSets List of parameter sets
     * @return BatchQuery instance for INSERT operations
     */
    public static BatchQuery insert(String sql, List<List<Object>> parameterSets) {
        return new BatchQuery(sql, parameterSets, true, Query.QueryType.INSERT, parameterSets.size());
    }
    
    /**
     * Create batch UPDATE query
     * @param sql UPDATE statement
     * @param parameterSets List of parameter sets
     * @return BatchQuery instance for UPDATE operations
     */
    public static BatchQuery update(String sql, List<List<Object>> parameterSets) {
        return new BatchQuery(sql, parameterSets, false, Query.QueryType.UPDATE, parameterSets.size());
    }
    
    /**
     * Create batch DELETE query
     * @param sql DELETE statement
     * @param parameterSets List of parameter sets
     * @return BatchQuery instance for DELETE operations
     */
    public static BatchQuery delete(String sql, List<List<Object>> parameterSets) {
        return new BatchQuery(sql, parameterSets, false, Query.QueryType.DELETE, parameterSets.size());
    }
    
    /**
     * Create empty batch query
     * @param sql SQL statement
     * @param type Query type
     * @return Empty BatchQuery instance
     */
    public static BatchQuery empty(String sql, Query.QueryType type) {
        return new BatchQuery(sql, List.of(), false, type, 0);
    }
    
    /**
     * Get total number of operations in batch
     * @return Number of parameter sets
     */
    public int getBatchSize() {
        return parameterSets.size();
    }
    
    /**
     * Check if batch is empty
     * @return true if no operations in batch
     */
    public boolean isEmpty() {
        return parameterSets.isEmpty();
    }
    
    /**
     * Check if batch has operations
     * @return true if batch contains operations
     */
    public boolean hasOperations() {
        return !parameterSets.isEmpty();
    }
    
    /**
     * Get parameter set at specific index
     * @param index Index of parameter set
     * @return Parameter set at index
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public List<Object> getParameterSet(int index) {
        return parameterSets.get(index);
    }
    
    /**
     * Get total number of parameters across all sets
     * @return Total parameter count
     */
    public int getTotalParameterCount() {
        return parameterSets.stream()
            .mapToInt(List::size)
            .sum();
    }
    
    /**
     * Check if all parameter sets have the same size
     * @return true if all parameter sets are uniform
     */
    public boolean hasUniformParameterSets() {
        if (parameterSets.isEmpty()) {
            return true;
        }
        
        var firstSize = parameterSets.get(0).size();
        return parameterSets.stream()
            .allMatch(params -> params.size() == firstSize);
    }
    
    /**
     * Get parameter count of first set (assuming uniform sets)
     * @return Parameter count per set
     * @throws IllegalStateException if batch is empty
     */
    public int getParameterCountPerSet() {
        if (parameterSets.isEmpty()) {
            throw new IllegalStateException("Cannot get parameter count from empty batch");
        }
        return parameterSets.get(0).size();
    }
    
    /**
     * Check if query is a batch INSERT
     * @return true if query type is INSERT
     */
    public boolean isInsert() {
        return type == Query.QueryType.INSERT;
    }
    
    /**
     * Check if query is a batch UPDATE
     * @return true if query type is UPDATE
     */
    public boolean isUpdate() {
        return type == Query.QueryType.UPDATE;
    }
    
    /**
     * Check if query is a batch DELETE
     * @return true if query type is DELETE
     */
    public boolean isDelete() {
        return type == Query.QueryType.DELETE;
    }
    
    /**
     * Split batch into smaller batches
     * @param maxBatchSize Maximum size per batch
     * @return List of smaller BatchQuery instances
     */
    public List<BatchQuery> split(int maxBatchSize) {
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("Max batch size must be positive");
        }
        
        if (parameterSets.size() <= maxBatchSize) {
            return List.of(this);
        }
        
        var result = new java.util.ArrayList<BatchQuery>();
        
        for (int i = 0; i < parameterSets.size(); i += maxBatchSize) {
            var endIndex = Math.min(i + maxBatchSize, parameterSets.size());
            var subList = parameterSets.subList(i, endIndex);
            
            result.add(new BatchQuery(sql, subList, returnGeneratedKeys, type, subList.size()));
        }
        
        return result;
    }
    
    /**
     * Get formatted debug string
     * @return Formatted batch query information
     */
    public String getDebugString() {
        return String.format(
            "BatchQuery[sql=%s, batchSize=%d, type=%s, returnGeneratedKeys=%s]",
            sql, batchSize, type, returnGeneratedKeys
        );
    }
    
    /**
     * Infer query type from SQL statement
     * @param sql SQL statement
     * @return Inferred query type
     */
    private static Query.QueryType inferQueryType(String sql) {
        var trimmedSql = sql.trim().toUpperCase();
        
        if (trimmedSql.startsWith("INSERT")) {
            return Query.QueryType.INSERT;
        } else if (trimmedSql.startsWith("UPDATE")) {
            return Query.QueryType.UPDATE;
        } else if (trimmedSql.startsWith("DELETE")) {
            return Query.QueryType.DELETE;
        } else {
            return Query.QueryType.UTILITY;
        }
    }
}