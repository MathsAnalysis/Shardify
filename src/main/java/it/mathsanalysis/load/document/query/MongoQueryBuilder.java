package it.mathsanalysis.load.document.query;

import it.mathsanalysis.load.spi.query.DocumentQueryBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB-specific query builder implementation
 * Builds MongoDB queries using BSON-like structures
 */
public final class MongoQueryBuilder<T> implements DocumentQueryBuilder<T> {

    private final Map<String, Object> queryStats = new ConcurrentHashMap<>();

    public MongoQueryBuilder() {
        initializeStats();
    }

    @Override
    public Object buildFindByIdQuery(Object id) {
        recordQuery("findById");
        return Map.of("_id", id);
    }

    @Override
    public Object buildInsertQuery(T item, Map<String, Object> parameters) {
        recordQuery("insert");
        return Map.of(
                "operation", "insertOne",
                "parameters", parameters != null ? parameters : Map.of()
        );
    }

    @Override
    public Object buildUpdateQuery(T item, Map<String, Object> parameters) {
        recordQuery("update");
        return Map.of(
                "operation", "updateOne",
                "parameters", parameters != null ? parameters : Map.of()
        );
    }

    @Override
    public Object buildDeleteQuery(T item, Map<String, Object> parameters) {
        recordQuery("delete");
        return Map.of(
                "operation", "deleteOne",
                "parameters", parameters != null ? parameters : Map.of()
        );
    }

    @Override
    public Object buildFindQuery(Map<String, Object> criteria, Map<String, Object> parameters) {
        recordQuery("find");
        var query = new java.util.HashMap<>(criteria);

        // Add MongoDB-specific query parameters
        if (parameters != null) {
            if (parameters.containsKey("sort")) {
                query.put("$sort", parameters.get("sort"));
            }
            if (parameters.containsKey("limit")) {
                query.put("$limit", parameters.get("limit"));
            }
            if (parameters.containsKey("skip")) {
                query.put("$skip", parameters.get("skip"));
            }
        }

        return query;
    }

    @Override
    public Object buildAggregationQuery(List<Map<String, Object>> pipeline, Map<String, Object> parameters) {
        recordQuery("aggregate");
        var aggregation = new java.util.HashMap<String, Object>();
        aggregation.put("pipeline", pipeline);

        if (parameters != null) {
            aggregation.putAll(parameters);
        }

        return aggregation;
    }

    @Override
    public Object buildCountQuery(Map<String, Object> criteria) {
        recordQuery("count");
        return Map.of(
                "operation", "countDocuments",
                "filter", criteria
        );
    }

    @Override
    public Object buildDistinctQuery(String fieldName, Map<String, Object> criteria) {
        recordQuery("distinct");
        return Map.of(
                "operation", "distinct",
                "field", fieldName,
                "filter", criteria != null ? criteria : Map.of()
        );
    }

    @Override
    public Object buildCreateIndexQuery(Map<String, Object> indexSpec, Map<String, Object> options) {
        recordQuery("createIndex");
        return Map.of(
                "operation", "createIndex",
                "keys", indexSpec,
                "options", options != null ? options : Map.of()
        );
    }

    @Override
    public Object buildTextSearchQuery(String searchText, Map<String, Object> parameters) {
        recordQuery("textSearch");
        Map query = Map.of("$text", Map.of("$search", searchText));

        if (parameters != null && parameters.containsKey("language")) {
            ((Map<String, Object>) query.get("$text")).put("$language", parameters.get("language"));
        }

        return query;
    }

    @Override
    public Object buildGeospatialQuery(Map<String, Object> location, Map<String, Object> parameters) {
        recordQuery("geoSearch");
        var geoQuery = new java.util.HashMap<String, Object>();

        var locationField = parameters != null ?
                (String) parameters.getOrDefault("locationField", "location") : "location";

        if (parameters != null && parameters.containsKey("maxDistance")) {
            geoQuery.put(locationField, Map.of(
                    "$near", Map.of(
                            "$geometry", location,
                            "$maxDistance", parameters.get("maxDistance")
                    )
            ));
        } else {
            geoQuery.put(locationField, Map.of("$near", location));
        }

        return geoQuery;
    }

    @Override
    public Object buildRegexQuery(String field, String pattern, Map<String, Object> options) {
        recordQuery("regex");
        var regexOptions = options != null ?
                (String) options.getOrDefault("options", "i") : "i";

        return Map.of(field, Map.of(
                "$regex", pattern,
                "$options", regexOptions
        ));
    }

    @Override
    public String getCollectionName() {
        return "mongodb_collection";
    }

    @Override
    public String getDatabaseType() {
        return "MongoDB";
    }

    @Override
    public Map<String, Object> getQueryStats() {
        return Map.copyOf(queryStats);
    }

    @Override
    public void resetQueryStats() {
        queryStats.clear();
        initializeStats();
    }

    // MongoDB-specific query builders

    /**
     * Build match stage for aggregation pipeline
     */
    public Map<String, Object> buildMatchStage(Map<String, Object> criteria) {
        return Map.of("$match", criteria);
    }

    /**
     * Build group stage for aggregation pipeline
     */
    public Map<String, Object> buildGroupStage(String groupField, Map<String, Object> accumulator) {
        return Map.of("$group", Map.of(
                "_id", "$" + groupField,
                "result", accumulator
        ));
    }

    /**
     * Build sort stage for aggregation pipeline
     */
    public Map<String, Object> buildSortStage(Map<String, Object> sortFields) {
        return Map.of("$sort", sortFields);
    }

    /**
     * Build limit stage for aggregation pipeline
     */
    public Map<String, Object> buildLimitStage(int limit) {
        return Map.of("$limit", limit);
    }

    /**
     * Build skip stage for aggregation pipeline
     */
    public Map<String, Object> buildSkipStage(int skip) {
        return Map.of("$skip", skip);
    }

    /**
     * Build projection stage for aggregation pipeline
     */
    public Map<String, Object> buildProjectStage(Map<String, Object> projection) {
        return Map.of("$project", projection);
    }

    /**
     * Build lookup stage for aggregation pipeline (JOIN equivalent)
     */
    public Map<String, Object> buildLookupStage(
            String fromCollection,
            String localField,
            String foreignField,
            String asField) {
        return Map.of("$lookup", Map.of(
                "from", fromCollection,
                "localField", localField,
                "foreignField", foreignField,
                "as", asField
        ));
    }

    /**
     * Build unwind stage for aggregation pipeline
     */
    public Map<String, Object> buildUnwindStage(String field, boolean preserveNullAndEmptyArrays) {
        return Map.of("$unwind", Map.of(
                "path", "$" + field,
                "preserveNullAndEmptyArrays", preserveNullAndEmptyArrays
        ));
    }

    /**
     * Build compound query with AND logic
     */
    public Map<String, Object> buildAndQuery(List<Map<String, Object>> conditions) {
        return Map.of("$and", conditions);
    }

    /**
     * Build compound query with OR logic
     */
    public Map<String, Object> buildOrQuery(List<Map<String, Object>> conditions) {
        return Map.of("$or", conditions);
    }

    /**
     * Build range query
     */
    public Map<String, Object> buildRangeQuery(String field, Object min, Object max) {
        var rangeCondition = new java.util.HashMap<String, Object>();
        if (min != null) rangeCondition.put("$gte", min);
        if (max != null) rangeCondition.put("$lte", max);

        return Map.of(field, rangeCondition);
    }

    /**
     * Build in query
     */
    public Map<String, Object> buildInQuery(String field, List<Object> values) {
        return Map.of(field, Map.of("$in", values));
    }

    /**
     * Build exists query
     */
    public Map<String, Object> buildExistsQuery(String field, boolean exists) {
        return Map.of(field, Map.of("$exists", exists));
    }


    private void recordQuery(String queryType) {
        queryStats.merge(queryType + "_count", 1L, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });

        queryStats.put("last_query_type", queryType);
        queryStats.put("last_query_time", System.currentTimeMillis());
    }

    private void initializeStats() {
        queryStats.put("created_at", System.currentTimeMillis());
        queryStats.put("total_queries", 0L);
        queryStats.put("database_type", "MongoDB");
    }
}