package it.mathsanalysis.load.resilience.exception;

import java.util.Map;

public final class SerializationException extends DataLoaderException {
    
    public SerializationException(String message, Map<String, Object> context, Throwable cause) {
        super(message, SERIALIZATION_FAILED, ErrorSeverity.MEDIUM, context, cause);
    }
    
    public static SerializationException fieldMappingFailed(String fieldName, Class<?> sourceType, Class<?> targetType) {
        Map<String, Object> context = Map.of(
            "fieldName", fieldName,
            "sourceType", sourceType.getSimpleName(),
            "targetType", targetType.getSimpleName()
        );
        return new SerializationException("Field mapping failed", context, null);
    }
}

