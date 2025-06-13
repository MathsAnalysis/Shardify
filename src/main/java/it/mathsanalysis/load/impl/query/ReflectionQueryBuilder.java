// ===== Aggiornamento ReflectionQueryBuilder con metodi mancanti =====
package it.mathsanalysis.load.impl.query;

import it.mathsanalysis.load.spi.query.QueryBuilder;
import it.mathsanalysis.load.spi.query.model.Query;
import it.mathsanalysis.load.spi.query.model.BatchQuery;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Reflection-based query builder with all required methods
 * Auto-generates SQL queries from Java objects using reflection
 * Optimized with caching for repeated operations
 */
public final class ReflectionQueryBuilder<T> implements QueryBuilder<T> {

    private final Class<T> itemType;
    private final String tableName;
    private final boolean enableCache;
    private final Map<String, Query> queryCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> cacheStats = new ConcurrentHashMap<>();
    private final Map<String, Object> performanceStats = new ConcurrentHashMap<>();

    public ReflectionQueryBuilder(Class<T> itemType, String tableName, boolean enableCache) {
        this.itemType = itemType;
        this.tableName = tableName;
        this.enableCache = enableCache;
        initializeCacheStats();
        initializePerformanceStats();
    }

    // Existing methods...
    @Override
    public Query buildInsertQuery(T item, Map<String, Object> parameters) {
        var startTime = System.nanoTime();
        var cacheKey = "INSERT_" + itemType.getSimpleName();

        if (enableCache && queryCache.containsKey(cacheKey)) {
            cacheStats.merge("cache_hits", 1, Integer::sum);
            recordQueryPerformance("buildInsertQuery", System.nanoTime() - startTime);
            return queryCache.get(cacheKey);
        }

        var fields = getFields();
        var columns = String.join(", ", fields.stream().map(Field::getName).toList());
        var placeholders = String.join(", ", Collections.nCopies(fields.size(), "?"));

        var sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
        var query = Query.withGeneratedKeys(sql, extractFieldValues(item, fields));

        if (enableCache) {
            queryCache.put(cacheKey, query);
            cacheStats.merge("cache_misses", 1, Integer::sum);
        }

        recordQueryPerformance("buildInsertQuery", System.nanoTime() - startTime);
        return query;
    }

    @Override
    public BatchQuery buildBatchInsertQuery(List<T> items, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        if (items.isEmpty()) {
            return BatchQuery.empty("", Query.QueryType.INSERT);
        }

        var fields = getFields();
        var columns = String.join(", ", fields.stream().map(Field::getName).toList());
        var placeholders = String.join(", ", Collections.nCopies(fields.size(), "?"));

        var sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
        var parameterSets = items.stream()
                .map(item -> extractFieldValues(item, fields))
                .toList();

        recordQueryPerformance("buildBatchInsertQuery", System.nanoTime() - startTime);
        return BatchQuery.insert(sql, parameterSets);
    }

    @Override
    public Query buildUpdateQuery(T item, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        var fields = getFields();
        var setClauses = fields.stream()
                .filter(f -> !f.getName().equals("id"))
                .map(f -> f.getName() + " = ?")
                .toList();

        var sql = String.format("UPDATE %s SET %s WHERE id = ?",
                tableName, String.join(", ", setClauses));

        var params = new ArrayList<Object>();
        fields.stream()
                .filter(f -> !f.getName().equals("id"))
                .forEach(f -> params.add(getFieldValue(f, item)));
        params.add(getIdFieldValue(item));

        recordQueryPerformance("buildUpdateQuery", System.nanoTime() - startTime);
        return Query.update(sql, params);
    }

    @Override
    public Query buildDeleteQuery(T item, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        var sql = String.format("DELETE FROM %s WHERE id = ?", tableName);
        var query = Query.delete(sql, List.of(getIdFieldValue(item)));

        recordQueryPerformance("buildDeleteQuery", System.nanoTime() - startTime);
        return query;
    }

    @Override
    public Query buildSelectByIdQuery(Object id) {
        var startTime = System.nanoTime();

        var fields = getFields();
        var columns = String.join(", ", fields.stream().map(Field::getName).toList());
        var sql = String.format("SELECT %s FROM %s WHERE id = ?", columns, tableName);
        var query = Query.select(sql, List.of(id));

        recordQueryPerformance("buildSelectByIdQuery", System.nanoTime() - startTime);
        return query;
    }

    @Override
    public Query buildSelectQuery(Map<String, Object> criteria, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        var fields = getFields();
        var columns = String.join(", ", fields.stream().map(Field::getName).toList());

        if (criteria.isEmpty()) {
            var sql = String.format("SELECT %s FROM %s", columns, tableName);
            recordQueryPerformance("buildSelectQuery", System.nanoTime() - startTime);
            return Query.select(sql, List.of());
        }

        var whereClauses = criteria.keySet().stream()
                .map(key -> key + " = ?")
                .toList();

        var sql = String.format("SELECT %s FROM %s WHERE %s",
                columns, tableName, String.join(" AND ", whereClauses));

        recordQueryPerformance("buildSelectQuery", System.nanoTime() - startTime);
        return Query.select(sql, new ArrayList<>(criteria.values()));
    }

    @Override
    public Query buildCreateTableQuery(Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        var columns = (List<String>) parameters.get("columns");

        if (columns == null || columns.isEmpty()) {
            columns = generateColumnsFromReflection();
        }

        var columnDefinitions = String.join(", ", columns);
        var sql = String.format("CREATE TABLE IF NOT EXISTS %s (%s)", tableName, columnDefinitions);

        recordQueryPerformance("buildCreateTableQuery", System.nanoTime() - startTime);
        return Query.ddl(sql);
    }

    @Override
    public Query buildDropTableQuery(Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        var sql = String.format("DROP TABLE IF EXISTS %s", tableName);

        recordQueryPerformance("buildDropTableQuery", System.nanoTime() - startTime);
        return Query.ddl(sql);
    }

    @Override
    public Query buildCreateIndexQuery(Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        var indexName = (String) parameters.get("indexName");
        var columns = (List<String>) parameters.get("columns");

        var sql = String.format("CREATE INDEX %s ON %s (%s)",
                indexName, tableName, String.join(", ", columns));

        recordQueryPerformance("buildCreateIndexQuery", System.nanoTime() - startTime);
        return Query.ddl(sql);
    }

    @Override
    public Query buildDropIndexQuery(String indexName) {
        var startTime = System.nanoTime();

        var sql = String.format("DROP INDEX IF EXISTS %s", indexName);

        recordQueryPerformance("buildDropIndexQuery", System.nanoTime() - startTime);
        return Query.ddl(sql);
    }

    @Override
    public Query buildCountQuery(Map<String, Object> criteria) {
        var startTime = System.nanoTime();

        if (criteria.isEmpty()) {
            var sql = String.format("SELECT COUNT(*) FROM %s", tableName);
            recordQueryPerformance("buildCountQuery", System.nanoTime() - startTime);
            return Query.select(sql, List.of());
        }

        var whereClauses = criteria.keySet().stream()
                .map(key -> key + " = ?")
                .toList();

        var sql = String.format("SELECT COUNT(*) FROM %s WHERE %s",
                tableName, String.join(" AND ", whereClauses));

        recordQueryPerformance("buildCountQuery", System.nanoTime() - startTime);
        return Query.select(sql, new ArrayList<>(criteria.values()));
    }

    // ===== NEW METHODS - Previously missing =====

    @Override
    public Query buildExistsQuery(Map<String, Object> criteria) {
        var startTime = System.nanoTime();

        if (criteria.isEmpty()) {
            var sql = String.format("SELECT EXISTS(SELECT 1 FROM %s)", tableName);
            recordQueryPerformance("buildExistsQuery", System.nanoTime() - startTime);
            return Query.select(sql, List.of());
        }

        var whereClauses = criteria.keySet().stream()
                .map(key -> key + " = ?")
                .toList();

        var sql = String.format("SELECT EXISTS(SELECT 1 FROM %s WHERE %s)",
                tableName, String.join(" AND ", whereClauses));

        recordQueryPerformance("buildExistsQuery", System.nanoTime() - startTime);
        return Query.select(sql, new ArrayList<>(criteria.values()));
    }

    @Override
    public Query buildPaginatedQuery(Map<String, Object> criteria, int offset, int limit) {
        var startTime = System.nanoTime();

        var fields = getFields();
        var columns = String.join(", ", fields.stream().map(Field::getName).toList());

        var sql = new StringBuilder();
        var params = new ArrayList<Object>();

        sql.append(String.format("SELECT %s FROM %s", columns, tableName));

        if (!criteria.isEmpty()) {
            var whereClauses = criteria.keySet().stream()
                    .map(key -> key + " = ?")
                    .toList();
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            params.addAll(criteria.values());
        }

        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        recordQueryPerformance("buildPaginatedQuery", System.nanoTime() - startTime);
        return Query.select(sql.toString(), params);
    }

    @Override
    public Query buildOrderedQuery(Map<String, Object> criteria, List<String> orderBy) {
        var startTime = System.nanoTime();

        var fields = getFields();
        var columns = String.join(", ", fields.stream().map(Field::getName).toList());

        var sql = new StringBuilder();
        var params = new ArrayList<Object>();

        sql.append(String.format("SELECT %s FROM %s", columns, tableName));

        if (!criteria.isEmpty()) {
            var whereClauses = criteria.keySet().stream()
                    .map(key -> key + " = ?")
                    .toList();
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            params.addAll(criteria.values());
        }

        if (!orderBy.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderBy));
        }

        recordQueryPerformance("buildOrderedQuery", System.nanoTime() - startTime);
        return Query.select(sql.toString(), params);
    }

    @Override
    public Query buildSumQuery(String column, Map<String, Object> criteria) {
        var startTime = System.nanoTime();

        var sql = new StringBuilder();
        var params = new ArrayList<Object>();

        sql.append(String.format("SELECT SUM(%s) FROM %s", column, tableName));

        if (!criteria.isEmpty()) {
            var whereClauses = criteria.keySet().stream()
                    .map(key -> key + " = ?")
                    .toList();
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            params.addAll(criteria.values());
        }

        recordQueryPerformance("buildSumQuery", System.nanoTime() - startTime);
        return Query.select(sql.toString(), params);
    }

    @Override
    public Query buildAverageQuery(String column, Map<String, Object> criteria) {
        var startTime = System.nanoTime();

        var sql = new StringBuilder();
        var params = new ArrayList<Object>();

        sql.append(String.format("SELECT AVG(%s) FROM %s", column, tableName));

        if (!criteria.isEmpty()) {
            var whereClauses = criteria.keySet().stream()
                    .map(key -> key + " = ?")
                    .toList();
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            params.addAll(criteria.values());
        }

        recordQueryPerformance("buildAverageQuery", System.nanoTime() - startTime);
        return Query.select(sql.toString(), params);
    }

    @Override
    public Query buildMinMaxQuery(String column, Map<String, Object> criteria, boolean isMin) {
        var startTime = System.nanoTime();

        var function = isMin ? "MIN" : "MAX";
        var sql = new StringBuilder();
        var params = new ArrayList<Object>();

        sql.append(String.format("SELECT %s(%s) FROM %s", function, column, tableName));

        if (!criteria.isEmpty()) {
            var whereClauses = criteria.keySet().stream()
                    .map(key -> key + " = ?")
                    .toList();
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            params.addAll(criteria.values());
        }

        recordQueryPerformance("buildMinMaxQuery", System.nanoTime() - startTime);
        return Query.select(sql.toString(), params);
    }

    @Override
    public Query buildGroupByQuery(List<String> groupBy, Map<String, Object> criteria) {
        var startTime = System.nanoTime();

        var sql = new StringBuilder();
        var params = new ArrayList<Object>();

        sql.append(String.format("SELECT %s, COUNT(*) FROM %s",
                String.join(", ", groupBy), tableName));

        if (!criteria.isEmpty()) {
            var whereClauses = criteria.keySet().stream()
                    .map(key -> key + " = ?")
                    .toList();
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            params.addAll(criteria.values());
        }

        sql.append(" GROUP BY ").append(String.join(", ", groupBy));

        recordQueryPerformance("buildGroupByQuery", System.nanoTime() - startTime);
        return Query.select(sql.toString(), params);
    }

    @Override
    public Query buildJoinQuery(String joinTable, String joinCondition, Map<String, Object> criteria) {
        var startTime = System.nanoTime();

        var fields = getFields();
        var columns = String.join(", ", fields.stream()
                .map(f -> tableName + "." + f.getName())
                .toList());

        var sql = new StringBuilder();
        var params = new ArrayList<Object>();

        sql.append(String.format("SELECT %s FROM %s INNER JOIN %s ON %s",
                columns, tableName, joinTable, joinCondition));

        if (!criteria.isEmpty()) {
            var whereClauses = criteria.keySet().stream()
                    .map(key -> tableName + "." + key + " = ?")
                    .toList();
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            params.addAll(criteria.values());
        }

        recordQueryPerformance("buildJoinQuery", System.nanoTime() - startTime);
        return Query.select(sql.toString(), params);
    }

    @Override
    public Query buildLeftJoinQuery(String joinTable, String joinCondition, Map<String, Object> criteria) {
        var startTime = System.nanoTime();

        var fields = getFields();
        var columns = String.join(", ", fields.stream()
                .map(f -> tableName + "." + f.getName())
                .toList());

        var sql = new StringBuilder();
        var params = new ArrayList<Object>();

        sql.append(String.format("SELECT %s FROM %s LEFT JOIN %s ON %s",
                columns, tableName, joinTable, joinCondition));

        if (!criteria.isEmpty()) {
            var whereClauses = criteria.keySet().stream()
                    .map(key -> tableName + "." + key + " = ?")
                    .toList();
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            params.addAll(criteria.values());
        }

        recordQueryPerformance("buildLeftJoinQuery", System.nanoTime() - startTime);
        return Query.select(sql.toString(), params);
    }

    @Override
    public BatchQuery buildBatchUpdateQuery(List<T> items, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        if (items == null || items.isEmpty()) {
            return BatchQuery.empty("", Query.QueryType.UPDATE);
        }

        var fields = getFields();
        if (fields.isEmpty()) {
            throw new IllegalStateException("Error the fields cannot be null " + itemType.getSimpleName());
        }

        var setClauses = fields.stream()
                .filter(f -> !f.getName().equals("id"))
                .map(f -> f.getName() + " = ?")
                .toList();

        var sql = String.format("UPDATE %s SET %s WHERE id = ?",
                tableName, String.join(", ", setClauses));

        var parameterSets = items.stream().map(item -> {
            try {
                var params = new ArrayList<>();
                fields.stream().filter(f -> !f.getName().equals("id")).forEach(f -> {
                    var value = getFieldValue(f, item);
                    params.add(value);
                });

                var idValue = getIdFieldValue(item);
                if (idValue == null) {
                    throw new IllegalArgumentException("ID field cannot be null for item: " + item);
                }

                params.add(idValue);

                return (List<Object>) params;
            } catch (Exception e) {
                throw new RuntimeException("Error not valid item: " + item, e);
            }
        }).toList();

        if (sql.trim().isEmpty()) {
            throw new IllegalStateException("SQL query cannot be empty for batch update");
        }

        recordQueryPerformance("buildBatchUpdateQuery", System.nanoTime() - startTime);
        return BatchQuery.update(sql, parameterSets);
    }


    @Override
    public BatchQuery buildBatchDeleteQuery(List<T> items, Map<String, Object> parameters) {
        var startTime = System.nanoTime();

        if (items.isEmpty()) {
            return BatchQuery.empty("", Query.QueryType.DELETE);
        }

        var sql = String.format("DELETE FROM %s WHERE id = ?", tableName);
        var parameterSets = items.stream()
                .map(item -> List.<Object>of(getIdFieldValue(item)))
                .toList();

        recordQueryPerformance("buildBatchDeleteQuery", System.nanoTime() - startTime);
        return BatchQuery.delete(sql, parameterSets);
    }

    @Override
    public Query addQueryHint(Query query, String hint) {
        var hintedSql = query.sql() + " " + hint;
        return new Query(hintedSql, query.parameters(), query.returnGeneratedKeys(), query.type());
    }

    @Override
    public Query optimizeQuery(Query query, Map<String, Object> optimizationHints) {
        var optimizedSql = query.sql();

        // Apply optimization hints based on SQL dialect
        if (optimizationHints.containsKey("useIndex")) {
            var indexHint = (String) optimizationHints.get("useIndex");
            optimizedSql = optimizedSql.replace("FROM " + tableName,
                    "FROM " + tableName + " USE INDEX (" + indexHint + ")");
        }

        if (optimizationHints.containsKey("forceIndex")) {
            var indexHint = (String) optimizationHints.get("forceIndex");
            optimizedSql = optimizedSql.replace("FROM " + tableName,
                    "FROM " + tableName + " FORCE INDEX (" + indexHint + ")");
        }

        return new Query(optimizedSql, query.parameters(), query.returnGeneratedKeys(), query.type());
    }

    @Override
    public Query buildAlterTableQuery(Map<String, Object> alterations) {
        var startTime = System.nanoTime();

        var alterationType = (String) alterations.get("type");
        var sql = new StringBuilder("ALTER TABLE ").append(tableName);

        switch (alterationType.toUpperCase()) {
            case "ADD_COLUMN" -> {
                var columnName = (String) alterations.get("columnName");
                var columnType = (String) alterations.get("columnType");
                sql.append(" ADD COLUMN ").append(columnName).append(" ").append(columnType);
            }
            case "DROP_COLUMN" -> {
                var columnName = (String) alterations.get("columnName");
                sql.append(" DROP COLUMN ").append(columnName);
            }
            case "MODIFY_COLUMN" -> {
                var columnName = (String) alterations.get("columnName");
                var columnType = (String) alterations.get("columnType");
                sql.append(" MODIFY COLUMN ").append(columnName).append(" ").append(columnType);
            }
            default -> throw new IllegalArgumentException("Unsupported alteration type: " + alterationType);
        }

        recordQueryPerformance("buildAlterTableQuery", System.nanoTime() - startTime);
        return Query.ddl(sql.toString());
    }

    @Override
    public Query buildCreateConstraintQuery(Map<String, Object> constraint) {
        var startTime = System.nanoTime();

        var constraintName = (String) constraint.get("name");
        var constraintType = (String) constraint.get("type");
        var columns = (List<String>) constraint.get("columns");

        var sql = new StringBuilder("ALTER TABLE ").append(tableName).append(" ADD CONSTRAINT ").append(constraintName);

        switch (constraintType.toUpperCase()) {
            case "PRIMARY_KEY" -> sql.append(" PRIMARY KEY (").append(String.join(", ", columns)).append(")");
            case "FOREIGN_KEY" -> {
                var referencedTable = (String) constraint.get("referencedTable");
                var referencedColumns = (List<String>) constraint.get("referencedColumns");
                sql.append(" FOREIGN KEY (").append(String.join(", ", columns))
                        .append(") REFERENCES ").append(referencedTable)
                        .append(" (").append(String.join(", ", referencedColumns)).append(")");
            }
            case "UNIQUE" -> sql.append(" UNIQUE (").append(String.join(", ", columns)).append(")");
            case "CHECK" -> {
                var checkCondition = (String) constraint.get("condition");
                sql.append(" CHECK (").append(checkCondition).append(")");
            }
            default -> throw new IllegalArgumentException("Unsupported constraint type: " + constraintType);
        }

        recordQueryPerformance("buildCreateConstraintQuery", System.nanoTime() - startTime);
        return Query.ddl(sql.toString());
    }

    @Override
    public Query buildDropConstraintQuery(String constraintName) {
        var startTime = System.nanoTime();

        var sql = String.format("ALTER TABLE %s DROP CONSTRAINT %s", tableName, constraintName);

        recordQueryPerformance("buildDropConstraintQuery", System.nanoTime() - startTime);
        return Query.ddl(sql);
    }

    // ===== EXISTING METHODS =====

    @Override
    public Map<String, Integer> getCacheStats() {
        return Map.copyOf(cacheStats);
    }

    @Override
    public void clearCache() {
        queryCache.clear();
        cacheStats.replaceAll((k, v) -> 0);
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public String getSqlDialect() {
        return "Generic"; // Can be enhanced to detect dialect
    }

    @Override
    public void setCachingEnabled(boolean enabled) {
        // Implementation for dynamic cache control
        if (!enabled) {
            clearCache();
        }
    }

    @Override
    public boolean isCachingEnabled() {
        return enableCache;
    }

    @Override
    public Map<String, Object> getQueryPerformanceStats() {
        return Map.copyOf(performanceStats);
    }

    @Override
    public void resetQueryPerformanceStats() {
        performanceStats.clear();
        initializePerformanceStats();
    }


    private List<Field> getFields() {
        return Arrays.stream(itemType.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .peek(field -> field.setAccessible(true))
                .toList();
    }

    private List<Object> extractFieldValues(T item, List<Field> fields) {
        return fields.stream()
                .map(field -> getFieldValue(field, item))
                .toList();
    }

    private Object getFieldValue(Field field, T item) {
        try {
            field.setAccessible(true);
            return field.get(item);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access field: " + field.getName(), e);
        }
    }

    private Object getIdFieldValue(T item) {
        return getFields().stream()
                .filter(field -> field.getName().equals("id"))
                .findFirst()
                .map(field -> getFieldValue(field, item))
                .orElseThrow(() -> new RuntimeException("No ID field found"));
    }

    private List<String> generateColumnsFromReflection() {
        return getFields().stream()
                .map(field -> {
                    var columnName = field.getName();
                    var sqlType = mapJavaTypeToSql(field.getType());
                    var isId = field.getName().equals("id");

                    return isId ?
                            String.format("%s %s PRIMARY KEY", columnName, sqlType) :
                            String.format("%s %s", columnName, sqlType);
                })
                .toList();
    }

    private String mapJavaTypeToSql(Class<?> javaType) {
        return switch (javaType.getSimpleName()) {
            case "String" -> "VARCHAR(255)";
            case "Integer", "int" -> "INTEGER";
            case "Long", "long" -> "BIGINT";
            case "Boolean", "boolean" -> "BOOLEAN";
            case "Double", "double" -> "DOUBLE PRECISION";
            case "Float", "float" -> "REAL";
            case "BigDecimal" -> "DECIMAL(19,2)";
            case "LocalDate" -> "DATE";
            case "LocalDateTime" -> "TIMESTAMP";
            case "UUID" -> "UUID";
            default -> "TEXT";
        };
    }

    private void recordQueryPerformance(String operation, long durationNanos) {
        performanceStats.merge("queries_built", 1L, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });

        performanceStats.merge("total_build_time_ns", durationNanos, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });

        performanceStats.merge(operation + "_count", 1L, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });

        performanceStats.merge(operation + "_total_time_ns", durationNanos, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });

        // Calculate average
        var totalQueries = (Long) performanceStats.get("queries_built");
        var totalTime = (Long) performanceStats.get("total_build_time_ns");
        if (totalQueries != null && totalTime != null && totalQueries > 0) {
            performanceStats.put("average_build_time_ns", (double) totalTime / totalQueries);
        }
    }

    private void initializeCacheStats() {
        cacheStats.put("cache_hits", 0);
        cacheStats.put("cache_misses", 0);
    }

    private void initializePerformanceStats() {
        performanceStats.put("queries_built", 0L);
        performanceStats.put("total_build_time_ns", 0L);
        performanceStats.put("average_build_time_ns", 0.0);
        performanceStats.put("created_at", System.currentTimeMillis());
    }
}