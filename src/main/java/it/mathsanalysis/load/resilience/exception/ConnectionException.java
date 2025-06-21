package it.mathsanalysis.load.resilience.exception;

import java.util.Map;

public final class ConnectionException extends DataLoaderException {

    public ConnectionException(String message, Map<String, Object> context, Throwable cause) {
        super(message, CONNECTION_FAILED, ErrorSeverity.HIGH, context, cause);
    }

    public static ConnectionException poolExhausted(int activeConnections, int maxPoolSize) {
        Map<String, Object> context = Map.of(
                "activeConnections", activeConnections,
                "maxPoolSize", maxPoolSize,
                "timestamp", System.currentTimeMillis()
        );
        return new ConnectionException("Connection pool exhausted", context, null);
    }

    public static ConnectionException timeout(long timeoutMs) {
        Map<String, Object> context = Map.of(
                "timeoutMs", timeoutMs,
                "timestamp", System.currentTimeMillis()
        );
        return new ConnectionException("Connection timeout", context, null);
    }
}