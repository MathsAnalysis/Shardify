package it.mathsanalysis.load.resilience.exception;

import java.util.Map;

public final class QueryException extends DataLoaderException {

    public QueryException(String message, Map<String, Object> context, Throwable cause) {
        super(message, QUERY_FAILED, ErrorSeverity.HIGH, context, cause);
    }

    public static QueryException syntaxError(String sql, String errorDetails) {
        Map<String, Object> context = Map.of(
                "sql", sql,
                "errorDetails", errorDetails,
                "timestamp", System.currentTimeMillis()
        );
        return new QueryException("SQL syntax error", context, null);
    }
}
