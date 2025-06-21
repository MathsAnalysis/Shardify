package it.mathsanalysis.load.resilience.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Enhanced exception hierarchy for better error handling
 */

@Getter
public sealed class DataLoaderException extends RuntimeException permits ConnectionException, SerializationException, ValidationException, QueryException {

    private final String errorCode;
    private final Map<String, Object> context;
    private final ErrorSeverity severity;

    // Standardized error codes
    public static final String CONNECTION_FAILED = "DL_CONN_001";
    public static final String SERIALIZATION_FAILED = "DL_SER_001";
    public static final String VALIDATION_FAILED = "DL_VAL_001";
    public static final String QUERY_FAILED = "DL_QUERY_001";
    public static final String CIRCUIT_BREAKER_OPEN = "DL_CB_001";

    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public DataLoaderException(String message, String errorCode, ErrorSeverity severity, Map<String, Object> context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.severity = severity;
        this.context = context != null ? Map.copyOf(context) : Map.of();
    }

    public boolean isRetryable() {
        return switch (errorCode) {
            case CONNECTION_FAILED, CIRCUIT_BREAKER_OPEN -> true;
            default -> false;
        };
    }
}
