package it.mathsanalysis.load.plugin.structure;

import java.util.Map;
import java.util.Set;

/**
 * Base class per plugin di validazione
 */
public abstract class ValidationPlugin implements DataLoaderPlugin {

    @Override
    public Set<String> getSupportedDatabases() {
        return Set.of("*"); // Supporta tutti i database
    }

    @Override
    public <T> void beforeSave(T item, Map<String, Object> context) {
        var errors = validate(item);
        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors, context);
        }
    }

    protected abstract <T> java.util.List<String> validate(T item);
}
