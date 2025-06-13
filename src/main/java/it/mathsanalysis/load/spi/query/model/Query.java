package it.mathsanalysis.load.spi.query.model;

import java.util.List;
import java.util.Objects;

/**
 * Query container with SQL and parameters
 * Immutable and optimized for prepared statements
 * 
 * Value Object Pattern: Immutable data container for SQL queries
 */
public record Query(
    String sql,
    List<Object> parameters,
    boolean returnGeneratedKeys,
    QueryType type
) {
    
    /**
     * Query types for categorization and optimization
     */
    public enum QueryType {
        SELECT, INSERT, UPDATE, DELETE, DDL, UTILITY
    }
    
    /**
     * Compact constructor with validation
     */
    public Query {
        Objects.requireNonNull(sql, "SQL cannot be null");
        Objects.requireNonNull(parameters, "Parameters cannot be null");
        
        if (sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL cannot be empty");
        }
        
        // Make parameters immutable
        parameters = List.copyOf(parameters);
    }
    
    /**
     * Create simple query without parameters
     * @param sql SQL statement
     * @return Query instance
     */
    public static Query of(String sql) {
        return new Query(sql, List.of(), false, inferQueryType(sql));
    }
    
    /**
     * Create query with parameters
     * @param sql SQL statement
     * @param parameters Query parameters
     * @return Query instance
     */
    public static Query of(String sql, List<Object> parameters) {
        return new Query(sql, parameters, false, inferQueryType(sql));
    }
    
    /**
     * Create query with explicit type
     * @param sql SQL statement
     * @param parameters Query parameters
     * @param type Query type
     * @return Query instance
     */
    public static Query of(String sql, List<Object> parameters, QueryType type) {
        return new Query(sql, parameters, false, type);
    }
    
    /**
     * Create query that returns generated keys
     * @param sql SQL statement
     * @param parameters Query parameters
     * @return Query instance configured to return generated keys
     */
    public static Query withGeneratedKeys(String sql, List<Object> parameters) {
        return new Query(sql, parameters, true, inferQueryType(sql));
    }
    
    /**
     * Create SELECT query
     * @param sql SELECT statement
     * @param parameters Query parameters
     * @return SELECT query instance
     */
    public static Query select(String sql, List<Object> parameters) {
        return new Query(sql, parameters, false, QueryType.SELECT);
    }
    
    /**
     * Create INSERT query
     * @param sql INSERT statement
     * @param parameters Query parameters
     * @return INSERT query instance
     */
    public static Query insert(String sql, List<Object> parameters) {
        return new Query(sql, parameters, true, QueryType.INSERT);
    }
    
    /**
     * Create UPDATE query
     * @param sql UPDATE statement
     * @param parameters Query parameters
     * @return UPDATE query instance
     */
    public static Query update(String sql, List<Object> parameters) {
        return new Query(sql, parameters, false, QueryType.UPDATE);
    }
    
    /**
     * Create DELETE query
     * @param sql DELETE statement
     * @param parameters Query parameters
     * @return DELETE query instance
     */
    public static Query delete(String sql, List<Object> parameters) {
        return new Query(sql, parameters, false, QueryType.DELETE);
    }
    
    /**
     * Create DDL query
     * @param sql DDL statement
     * @return DDL query instance
     */
    public static Query ddl(String sql) {
        return new Query(sql, List.of(), false, QueryType.DDL);
    }
    
    /**
     * Get parameter count
     * @return Number of parameters
     */
    public int getParameterCount() {
        return parameters.size();
    }
    
    /**
     * Check if query has parameters
     * @return true if query has parameters
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }
    
    /**
     * Check if query is a SELECT
     * @return true if query type is SELECT
     */
    public boolean isSelect() {
        return type == QueryType.SELECT;
    }
    
    /**
     * Check if query is an INSERT
     * @return true if query type is INSERT
     */
    public boolean isInsert() {
        return type == QueryType.INSERT;
    }
    
    /**
     * Check if query is an UPDATE
     * @return true if query type is UPDATE
     */
    public boolean isUpdate() {
        return type == QueryType.UPDATE;
    }
    
    /**
     * Check if query is a DELETE
     * @return true if query type is DELETE
     */
    public boolean isDelete() {
        return type == QueryType.DELETE;
    }
    
    /**
     * Check if query is DDL
     * @return true if query type is DDL
     */
    public boolean isDdl() {
        return type == QueryType.DDL;
    }
    
    /**
     * Get formatted query string for debugging
     * @return Formatted query with parameters
     */
    public String getDebugString() {
        if (parameters.isEmpty()) {
            return sql;
        }
        return String.format("%s | Parameters: %s", sql, parameters);
    }
    
    /**
     * Infer query type from SQL statement
     * @param sql SQL statement
     * @return Inferred query type
     */
    private static QueryType inferQueryType(String sql) {
        var trimmedSql = sql.trim().toUpperCase();
        
        if (trimmedSql.startsWith("SELECT")) {
            return QueryType.SELECT;
        } else if (trimmedSql.startsWith("INSERT")) {
            return QueryType.INSERT;
        } else if (trimmedSql.startsWith("UPDATE")) {
            return QueryType.UPDATE;
        } else if (trimmedSql.startsWith("DELETE")) {
            return QueryType.DELETE;
        } else if (trimmedSql.startsWith("CREATE") || 
                   trimmedSql.startsWith("DROP") || 
                   trimmedSql.startsWith("ALTER")) {
            return QueryType.DDL;
        } else {
            return QueryType.UTILITY;
        }
    }
}