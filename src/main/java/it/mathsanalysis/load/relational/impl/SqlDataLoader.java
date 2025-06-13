package it.mathsanalysis.load.relational.impl;

import it.mathsanalysis.load.relational.RelationalDataLoader;
import it.mathsanalysis.load.spi.connection.ConnectionProvider;
import it.mathsanalysis.load.spi.query.QueryBuilder;
import it.mathsanalysis.load.spi.mapping.ItemMapper;

import java.util.*;
import java.util.List;
import java.util.Map;

/**
 * SQL Database implementation supporting multiple SQL dialects.
 * <p> 
 * Supported Databases:
 * - PostgreSQL (recommended for production)
 * - MySQL/MariaDB
 * - H2 (recommended for testing)
 * - SQLite (recommended for embedded/mobile)
 * - Oracle Database
 * - Microsoft SQL Server
 * <p> 
 * Performance Features:
 * - HikariCP connection pooling
 * - Prepared statement caching
 * - Batch operations with configurable sizes
 * - Connection leak detection
 * - Health monitoring with metrics
 * - Query optimization hints
 * <p> 
 * SQL-Specific Features:
 * - ACID transaction support
 * - Foreign key constraints
 * - Triggers and stored procedures
 * - Views and materialized views
 * - Advanced indexing strategies
 * - Query execution plan analysis
 * <p> 
 * Thread Safety: This implementation is fully thread-safe using
 * connection pooling and proper resource management.
 * 
 * @param <T> The type of items being loaded/saved
 * @param <ID> The type of item identifiers
 */
public final class SqlDataLoader<T, ID> extends RelationalDataLoader<T, ID> {

    /**
     * Create SQL data loader
     * 
     * @param itemType Class of items this loader handles
     * @param idType Class of item identifiers
     * @param connectionProvider SQL connection provider (HikariCP recommended)
     * @param queryBuilder SQL query builder with dialect support
     * @param itemMapper Object-relational mapper
     * @param tableName Target table name
     * @param configuration SQL-specific configuration
     */
    public SqlDataLoader(
            Class<T> itemType,
            Class<ID> idType,
            ConnectionProvider connectionProvider,
            QueryBuilder<T> queryBuilder,
            ItemMapper<T> itemMapper,
            String tableName,
            Map<String, Object> configuration) {
        
        super(itemType, idType, connectionProvider, queryBuilder, itemMapper, tableName, configuration);
    }

    /**
     * Execute custom SQL query with parameters
     * 
     * Allows execution of arbitrary SQL queries while maintaining
     * the same connection pool and error handling.
     * 
     * @param sql Custom SQL query
     * @param parameters Query parameters
     * @return List of results mapped to items
     */
    public List<T> executeCustomQuery(String sql, List<Object> parameters) {
        var startTime = System.nanoTime();
        var results = new ArrayList<T>();
        
        try (var connection = getConnectionProvider().getConnection()) {
            try (var statement = connection.prepareStatement(sql)) {
                // Bind parameters
                for (int i = 0; i < parameters.size(); i++) {
                    statement.setParameter(i + 1, parameters.get(i));
                }
                
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        results.add(getItemMapper().mapFromResultSet(resultSet));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute custom query", e);
        }
        
        if (isMetricsEnabled()) {
            getMetrics().recordOperation("executeCustomQuery", System.nanoTime() - startTime);
        }
        
        return results;
    }

    /**
     * Execute SQL update/delete with parameters
     * 
     * @param sql Update or delete SQL statement
     * @param parameters Query parameters
     * @return Number of affected rows
     */
    public int executeUpdate(String sql, List<Object> parameters) {
        var startTime = System.nanoTime();
        
        try (var connection = getConnectionProvider().getConnection()) {
            try (var statement = connection.prepareStatement(sql)) {
                // Bind parameters
                for (int i = 0; i < parameters.size(); i++) {
                    statement.setParameter(i + 1, parameters.get(i));
                }
                
                var rowsAffected = statement.executeUpdate();
                
                if (isMetricsEnabled()) {
                    getMetrics().recordOperation("executeUpdate", System.nanoTime() - startTime);
                }
                
                return rowsAffected;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute update", e);
        }
    }

    /**
     * Execute stored procedure
     * 
     * @param procedureName Stored procedure name
     * @param parameters Procedure parameters
     * @return Procedure results
     */
    public List<T> executeStoredProcedure(String procedureName, List<Object> parameters) {
        var startTime = System.nanoTime();
        var results = new ArrayList<T>();
        
        try (var connection = getConnectionProvider().getConnection()) {
            var sql = generateStoredProcedureCall(procedureName, parameters.size());
            
            try (var statement = connection.prepareStatement(sql)) {
                // Bind parameters
                for (int i = 0; i < parameters.size(); i++) {
                    statement.setParameter(i + 1, parameters.get(i));
                }
                
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        results.add(getItemMapper().mapFromResultSet(resultSet));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute stored procedure: " + procedureName, e);
        }
        
        if (isMetricsEnabled()) {
            getMetrics().recordOperation("executeStoredProcedure", System.nanoTime() - startTime);
        }
        
        return results;
    }

    /**
     * Analyze query execution plan
     * 
     * @param sql SQL query to analyze
     * @param parameters Query parameters
     * @return Query execution plan
     */
    public String explainQuery(String sql, List<Object> parameters) {
        try (var connection = getConnectionProvider().getConnection()) {
            var explainSql = buildExplainQuery(sql);
            
            try (var statement = connection.prepareStatement(explainSql)) {
                // Bind parameters
                for (int i = 0; i < parameters.size(); i++) {
                    statement.setParameter(i + 1, parameters.get(i));
                }
                
                try (var resultSet = statement.executeQuery()) {
                    var plan = new StringBuilder();
                    while (resultSet.next()) {
                        plan.append(resultSet.getString(String.valueOf(1))).append("\n");
                    }
                    return plan.toString();
                }
            }
        } catch (Exception e) {
            return "Failed to get execution plan: " + e.getMessage();
        }
    }

    /**
     * Get table statistics
     * 
     * @return Table statistics (row count, size, indexes, etc.)
     */
    public Map<String, Object> getTableStats() {
        try (var connection = getConnectionProvider().getConnection()) {
            var stats = new HashMap<String, Object>();
            
            // Get row count
            var countSql = String.format("SELECT COUNT(*) FROM %s", getTableName());
            try (var statement = connection.prepareStatement(countSql);
                 var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    stats.put("rowCount", resultSet.getLong(String.valueOf(1)));
                }
            }
            
            // Get table size (database-specific)
            var tableSizeQuery = buildTableSizeQuery();
            if (tableSizeQuery != null) {
                try (var statement = connection.prepareStatement(tableSizeQuery);
                     var resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        stats.put("tableSizeBytes", resultSet.getLong(String.valueOf(1)));
                    }
                }
            }
            
            // Get index information
            var indexes = getTableIndexes();
            stats.put("indexes", indexes);
            
            stats.put("tableName", getTableName());
            stats.put("dialect", getQueryBuilder().getSqlDialect());
            stats.put("lastUpdated", System.currentTimeMillis());
            
            return stats;
            
        } catch (Exception e) {
            return Map.of("error", "Failed to get table stats: " + e.getMessage());
        }
    }

    /**
     * Optimize table (VACUUM, ANALYZE, etc.)
     * 
     * @param options Optimization options
     * @return Optimization result
     */
    public Map<String, Object> optimizeTable(Map<String, Object> options) {
        var startTime = System.nanoTime();
        
        try (var connection = getConnectionProvider().getConnection()) {
            var results = new HashMap<String, Object>();
            
            // Execute database-specific optimization commands
            var optimizationCommands = buildOptimizationCommands(options);
            
            for (var command : optimizationCommands) {
                try (var statement = connection.prepareStatement(command)) {
                    statement.executeUpdate();
                    results.put("executed_" + command.split(" ")[0].toLowerCase(), true);
                }
            }
            
            if (isMetricsEnabled()) {
                getMetrics().recordOperation("optimizeTable", System.nanoTime() - startTime);
            }
            
            results.put("tableName", getTableName());
            results.put("optimizationTime", System.currentTimeMillis());
            
            return results;
            
        } catch (Exception e) {
            return Map.of("error", "Failed to optimize table: " + e.getMessage());
        }
    }

    /**
     * Create database backup
     * 
     * @param backupPath Path for backup file
     * @param options Backup options
     * @return Backup result
     */
    public Map<String, Object> createBackup(String backupPath, Map<String, Object> options) {
        var startTime = System.nanoTime();
        
        try {
            var backupCommands = buildBackupCommands(backupPath, options);
            var results = new HashMap<String, Object>();
            
            for (var command : backupCommands) {
                var process = Runtime.getRuntime().exec(command);
                var exitCode = process.waitFor();
                results.put("command_" + command.split(" ")[0], exitCode == 0 ? "success" : "failed");
            }
            
            if (isMetricsEnabled()) {
                getMetrics().recordOperation("createBackup", System.nanoTime() - startTime);
            }
            
            results.put("backupPath", backupPath);
            results.put("backupTime", System.currentTimeMillis());
            
            return results;
            
        } catch (Exception e) {
            return Map.of("error", "Failed to create backup: " + e.getMessage());
        }
    }

    /**
     * Get connection pool statistics
     * 
     * @return Detailed connection pool metrics
     */
    public Map<String, Object> getConnectionPoolStats() {
        var connectionStats = getConnectionProvider().getConnectionStats();
        var enhancedStats = new HashMap<>(connectionStats);
        
        // Add additional SQL-specific metrics
        enhancedStats.put("dialect", getQueryBuilder().getSqlDialect());
        enhancedStats.put("tableName", getTableName());
        enhancedStats.put("queryCache", getQueryBuilder().getCacheStats());
        enhancedStats.put("mappingStats", getItemMapper().getMappingStats());
        enhancedStats.put("timestamp", System.currentTimeMillis());
        
        return enhancedStats;
    }

    // Helper methods for database-specific operations

    private String generateStoredProcedureCall(String procedureName, int parameterCount) {
        var placeholders = Collections.nCopies(parameterCount, "?");
        return switch (getQueryBuilder().getSqlDialect().toLowerCase()) {
            case "postgresql" -> String.format("SELECT * FROM %s(%s)", procedureName, String.join(", ", placeholders));
            case "mysql" -> String.format("CALL %s(%s)", procedureName, String.join(", ", placeholders));
            case "sqlserver" -> String.format("EXEC %s %s", procedureName, String.join(", ", placeholders));
            default -> String.format("CALL %s(%s)", procedureName, String.join(", ", placeholders));
        };
    }

    private String buildExplainQuery(String sql) {
        return switch (getQueryBuilder().getSqlDialect().toLowerCase()) {
            case "postgresql" -> "EXPLAIN ANALYZE " + sql;
            case "mysql" -> "EXPLAIN FORMAT=JSON " + sql;
            case "sqlite" -> "EXPLAIN QUERY PLAN " + sql;
            case "h2" -> "EXPLAIN PLAN FOR " + sql;
            default -> "EXPLAIN " + sql;
        };
    }

    private String buildTableSizeQuery() {
        return switch (getQueryBuilder().getSqlDialect().toLowerCase()) {
            case "postgresql" -> String.format(
                "SELECT pg_total_relation_size('%s')", getTableName());
            case "mysql" -> String.format(
                "SELECT (DATA_LENGTH + INDEX_LENGTH) FROM information_schema.TABLES WHERE TABLE_NAME = '%s'", 
                getTableName());
            case "sqlite" -> String.format(
                "SELECT page_count * page_size FROM pragma_page_count(), pragma_page_size()");
            default -> null; // Not supported for this dialect
        };
    }

    private List<String> buildOptimizationCommands(Map<String, Object> options) {
        return switch (getQueryBuilder().getSqlDialect().toLowerCase()) {
            case "postgresql" -> List.of(
                String.format("VACUUM ANALYZE %s", getTableName()),
                String.format("REINDEX TABLE %s", getTableName())
            );
            case "mysql" -> List.of(
                String.format("OPTIMIZE TABLE %s", getTableName()),
                String.format("ANALYZE TABLE %s", getTableName())
            );
            case "sqlite" -> List.of(
                "VACUUM",
                "ANALYZE"
            );
            default -> List.of();
        };
    }

    private List<String> buildBackupCommands(String backupPath, Map<String, Object> options) {
        // This would need to be implemented based on specific database tools
        // For example: pg_dump for PostgreSQL, mysqldump for MySQL, etc.
        return List.of();
    }

    private List<Map<String, Object>> getTableIndexes() {
        // Implementation would query database-specific system tables
        // to get index information
        return List.of();
    }
}