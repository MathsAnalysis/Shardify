package it.mathsanalysis.load.relational;

import it.mathsanalysis.load.core.AbstractDataLoader;
import it.mathsanalysis.load.core.result.HealthStatus;
import it.mathsanalysis.load.spi.connection.ConnectionProvider;
import it.mathsanalysis.load.spi.query.QueryBuilder;
import it.mathsanalysis.load.spi.mapping.ItemMapper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Relational database implementation using SQL operations.
 * 
 * Optimized for SQL databases like PostgreSQL, MySQL, H2, SQLite.
 * Features:
 * - Connection pooling with HikariCP
 * - Prepared statement caching
 * - Batch operations with configurable sizes
 * - Transaction management
 * - Auto-generated key handling
 * - SQL dialect support
 * 
 * Performance Optimizations:
 * - Reuses prepared statements
 * - Batches multiple operations
 * - Uses connection pooling
 * - Implements query result caching
 * - Minimizes object allocations
 * 
 * Thread Safety: This class is thread-safe and can be used concurrently
 * from multiple threads.
 * 
 * @param <T> The type of items being loaded/saved
 * @param <ID> The type of item identifiers
 */
public abstract class RelationalDataLoader<T, ID> extends AbstractDataLoader<T, ID> {

    protected final ConnectionProvider connectionProvider;
    protected final QueryBuilder<T> queryBuilder;
    protected final ItemMapper<T> itemMapper;
    protected final String tableName;

    /**
     * Constructor for relational data loader
     * 
     * @param itemType Class of items this loader handles
     * @param idType Class of item identifiers  
     * @param connectionProvider Database connection provider
     * @param queryBuilder SQL query builder
     * @param itemMapper Object-relational mapper
     * @param tableName Target table name
     * @param configuration Loader configuration
     */
    protected RelationalDataLoader(
            Class<T> itemType,
            Class<ID> idType,
            ConnectionProvider connectionProvider,
            QueryBuilder<T> queryBuilder,
            ItemMapper<T> itemMapper,
            String tableName,
            Map<String, Object> configuration) {
        
        super(itemType, idType, configuration);
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.queryBuilder = Objects.requireNonNull(queryBuilder, "Query builder cannot be null");
        this.itemMapper = Objects.requireNonNull(itemMapper, "Item mapper cannot be null");
        this.tableName = Objects.requireNonNull(tableName, "Table name cannot be null");
    }

    @Override
    protected final T doSave(T item, Map<String, Object> parameters) {
        try (var connection = connectionProvider.getConnection()) {
            // Build optimized INSERT query with generated key support
            var query = queryBuilder.buildInsertQuery(item, parameters);
            
            try (var statement = connection.prepareStatement(query.sql(), query.returnGeneratedKeys())) {
                // Bind parameters efficiently
                bindParameters(statement, query.parameters());
                
                // Execute insert
                var rowsAffected = statement.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Insert failed, no rows affected");
                }
                
                // Handle generated keys (ID, timestamps, etc.)
                if (query.returnGeneratedKeys()) {
                    try (var generatedKeys = statement.getGeneratedKeys()) {
                        return itemMapper.mapFromResultSet(generatedKeys, item);
                    }
                }
                
                return item;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save item to table: " + tableName, e);
        }
    }

    @Override
    protected final List<T> doSaveBatch(List<T> items, Map<String, Object> parameters) {
        if (items.isEmpty()) {
            return List.of();
        }

        try (var connection = connectionProvider.getConnection()) {
            // Enable transaction for batch consistency
            var originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            
            try {
                // Build batch INSERT query
                var batchQuery = queryBuilder.buildBatchInsertQuery(items, parameters);
                
                try (var statement = connection.prepareStatement(batchQuery.sql(), batchQuery.returnGeneratedKeys())) {
                    
                    // Add all parameter sets to batch
                    for (var parameterSet : batchQuery.parameterSets()) {
                        bindParameters(statement, parameterSet);
                        statement.addBatch();
                    }
                    
                    // Execute batch
                    var updateCounts = statement.executeBatch();
                    
                    // Verify all inserts succeeded
                    for (var updateCount : updateCounts) {
                        if (updateCount == 0) {
                            throw new SQLException("Batch insert failed, some rows not affected");
                        }
                    }
                    
                    // Handle generated keys for batch
                    var result = new ArrayList<T>();
                    if (batchQuery.returnGeneratedKeys()) {
                        try (var generatedKeys = statement.getGeneratedKeys()) {
                            result.addAll(itemMapper.mapBatchFromResultSet(generatedKeys, items));
                        }
                    } else {
                        result.addAll(items);
                    }
                    
                    // Commit transaction
                    connection.commit();
                    return result;
                }
                
            } catch (Exception e) {
                // Rollback on any error
                connection.rollback();
                throw e;
            } finally {
                // Restore original auto-commit
                connection.setAutoCommit(originalAutoCommit);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save batch to table: " + tableName, e);
        }
    }

    @Override
    protected final Optional<T> doFindById(ID id) {
        try (var connection = connectionProvider.getConnection()) {
            // Build optimized SELECT by ID query
            var query = queryBuilder.buildSelectByIdQuery(id);
            
            try (var statement = connection.prepareStatement(query.sql())) {
                // Bind ID parameter
                bindParameters(statement, query.parameters());
                
                // Execute query
                try (var resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        // Map single result
                        var item = itemMapper.mapFromResultSet(resultSet);
                        return Optional.of(item);
                    } else {
                        return Optional.empty();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find item by ID in table: " + tableName, e);
        }
    }

    @Override
    protected final void doInitializeStorage(Map<String, Object> parameters) {
        try (var connection = connectionProvider.getConnection()) {
            // Build CREATE TABLE query
            var createTableQuery = queryBuilder.buildCreateTableQuery(parameters);
            
            try (var statement = connection.prepareStatement(createTableQuery.sql())) {
                // Execute table creation
                statement.executeUpdate();
            }
            
            // Create indexes if specified
            var indexes = (List<Map<String, Object>>) parameters.get("indexes");
            if (indexes != null) {
                for (var indexSpec : indexes) {
                    var indexQuery = queryBuilder.buildCreateIndexQuery(indexSpec);
                    try (var statement = connection.prepareStatement(indexQuery.sql())) {
                        statement.executeUpdate();
                    }
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize storage for table: " + tableName, e);
        }
    }

    @Override
    protected final HealthStatus doHealthCheck() {
        try {
            // Test connection validity
            if (!connectionProvider.isHealthy(5)) {
                return HealthStatus.unhealthy("Connection provider is not healthy", getConnectionStats());
            }
            
            // Test basic query execution
            try (var connection = connectionProvider.getConnection()) {
                var testQuery = getTestQuery();
                
                try (var statement = connection.prepareStatement(testQuery);
                     var resultSet = statement.executeQuery()) {
                    
                    if (resultSet.next()) {
                        Map<String, Object> healthMetrics = Map.of(
                            "tableName", tableName,
                            "connectionPoolSize", connectionProvider.getActiveConnections(),
                            "maxPoolSize", connectionProvider.getMaxPoolSize(),
                            "idleConnections", connectionProvider.getIdleConnections(),
                            "sqlDialect", queryBuilder.getSqlDialect(),
                            "lastHealthCheck", System.currentTimeMillis()
                        );
                        
                        return HealthStatus.healthy(healthMetrics, "Relational database is healthy");
                    } else {
                        return HealthStatus.unhealthy("Test query returned no results", getConnectionStats());
                    }
                }
            }
            
        } catch (Exception e) {
            Map<String, Object> errorMetrics = Map.of(
                "error", e.getMessage(),
                "errorType", e.getClass().getSimpleName(),
                "tableName", tableName,
                "timestamp", System.currentTimeMillis()
            );
            
            return HealthStatus.unhealthy("Health check failed: " + e.getMessage(), errorMetrics);
        }
    }

    @Override
    protected final Map<String, Object> getConnectionStats() {
        return connectionProvider.getConnectionStats();
    }

    // Additional relational-specific methods

    /**
     * Find items by custom criteria
     * 
     * @param criteria Search criteria map
     * @param parameters Additional query parameters
     * @return List of matching items
     */
    public List<T> findByCriteria(Map<String, Object> criteria, Map<String, Object> parameters) {
        var startTime = System.nanoTime();
        var results = new ArrayList<T>();
        
        try (var connection = connectionProvider.getConnection()) {
            var query = queryBuilder.buildSelectQuery(criteria, parameters);
            
            try (var statement = connection.prepareStatement(query.sql())) {
                bindParameters(statement, query.parameters());
                
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        results.add(itemMapper.mapFromResultSet(resultSet));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find items by criteria in table: " + tableName, e);
        }
        
        if (isMetricsEnabled()) {
            getMetrics().recordOperation("findByCriteria", System.nanoTime() - startTime);
        }
        
        return results;
    }

    /**
     * Count items by criteria
     * 
     * @param criteria Count criteria
     * @return Number of matching items
     */
    public long countByCriteria(Map<String, Object> criteria) {
        var startTime = System.nanoTime();
        
        try (var connection = connectionProvider.getConnection()) {
            var query = queryBuilder.buildCountQuery(criteria);
            
            try (var statement = connection.prepareStatement(query.sql())) {
                bindParameters(statement, query.parameters());
                
                try (var resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        var count = resultSet.getLong(String.valueOf(1));
                        
                        if (isMetricsEnabled()) {
                            getMetrics().recordOperation("countByCriteria", System.nanoTime() - startTime);
                        }
                        
                        return count;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count items in table: " + tableName, e);
        }
        
        return 0;
    }

    /**
     * Update item by ID
     * 
     * @param item Item with updated values
     * @param parameters Update parameters
     * @return Updated item
     */
    public T update(T item, Map<String, Object> parameters) {
        var startTime = System.nanoTime();
        
        try (var connection = connectionProvider.getConnection()) {
            var query = queryBuilder.buildUpdateQuery(item, parameters);
            
            try (var statement = connection.prepareStatement(query.sql())) {
                bindParameters(statement, query.parameters());
                
                var rowsAffected = statement.executeUpdate();
                
                if (isMetricsEnabled()) {
                    getMetrics().recordOperation("update", System.nanoTime() - startTime);
                }
                
                if (rowsAffected > 0) {
                    return item;
                } else {
                    throw new RuntimeException("Update affected no rows");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update item in table: " + tableName, e);
        }
    }

    /**
     * Delete item by ID
     * 
     * @param id ID of item to delete
     * @return true if item was deleted
     */
    public boolean deleteById(ID id) {
        if (id == null) {
            return false;
        }
        
        var startTime = System.nanoTime();
        
        try (var connection = connectionProvider.getConnection()) {
            var deleteQuery = String.format("DELETE FROM %s WHERE %s = ?", 
                tableName, itemMapper.getPrimaryKeyField());
            
            try (var statement = connection.prepareStatement(deleteQuery)) {
                statement.setParameter(1, id);
                
                var rowsAffected = statement.executeUpdate();
                
                if (isMetricsEnabled()) {
                    getMetrics().recordOperation("deleteById", System.nanoTime() - startTime);
                }
                
                return rowsAffected > 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete item from table: " + tableName, e);
        }
    }

    // Getters for subclasses
    protected final String getTableName() { return tableName; }
    protected final ConnectionProvider getConnectionProvider() { return connectionProvider; }
    protected final QueryBuilder<T> getQueryBuilder() { return queryBuilder; }
    protected final ItemMapper<T> getItemMapper() { return itemMapper; }

    // Helper methods
    private void bindParameters(
            it.mathsanalysis.load.spi.database.PreparedStatement statement, 
            List<Object> parameters) throws SQLException {
        
        for (int i = 0; i < parameters.size(); i++) {
            statement.setParameter(i + 1, parameters.get(i));
        }
    }

    private String getTestQuery() {
        // SQL dialect-specific test queries
        return switch (queryBuilder.getSqlDialect().toLowerCase()) {
            case "postgresql" -> "SELECT 1";
            case "mysql" -> "SELECT 1";
            case "h2" -> "SELECT 1";
            case "sqlite" -> "SELECT 1";
            default -> "SELECT 1";
        };
    }
}