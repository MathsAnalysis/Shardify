package it.mathsanalysis.load.resilience.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public final class ValidationException extends DataLoaderException {

    private final List<String> validationErrors;

    public ValidationException(String message, List<String> validationErrors, Map<String, Object> context) {
        super(message, VALIDATION_FAILED, ErrorSeverity.MEDIUM, context, null);
        this.validationErrors = List.copyOf(validationErrors);
    }

    public static ValidationException fromErrors(List<String> errors) {
        var context = Map.of(
                "errorCount", errors.size(),
                "errors", errors
        );
        return new ValidationException("Validation failed", errors, context);
    }
}
