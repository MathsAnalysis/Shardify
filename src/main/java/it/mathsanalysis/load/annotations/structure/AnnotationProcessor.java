package it.mathsanalysis.load.annotations.structure;

import it.mathsanalysis.load.annotations.impl.NotNull;
import it.mathsanalysis.load.annotations.impl.Pattern;
import it.mathsanalysis.load.annotations.impl.Size;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Processes annotations on classes and fields
 */
public class AnnotationProcessor {
    
    public <T> ValidationResult validate(T item) {
        List<String> errors = new ArrayList<>();
        
        for (Field field : item.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(item);
                validateField(field, value, errors);
            } catch (IllegalAccessException e) {
                errors.add("Cannot access field: " + field.getName());
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    private void validateField(Field field, Object value, List<String> errors) {
        // @NotNull validation
        if (field.isAnnotationPresent(NotNull.class) && value == null) {
            var annotation = field.getAnnotation(NotNull.class);
            errors.add(Objects.requireNonNull(annotation).message() + " (field: " + field.getName() + ")");
        }
        
        // @Size validation
        if (field.isAnnotationPresent(Size.class) && value instanceof String str) {
            var annotation = field.getAnnotation(Size.class);
            if (str.length() < Objects.requireNonNull(annotation).min() || str.length() > annotation.max()) {
                errors.add(annotation.message() + " (field: " + field.getName() + ")");
            }
        }
        
        // @Pattern validation
        if (field.isAnnotationPresent(Pattern.class) && value instanceof String str) {
            var annotation = field.getAnnotation(Pattern.class);
            if (!str.matches(annotation.value())) {
                errors.add(annotation.message() + " (field: " + field.getName() + ")");
            }
        }
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = List.copyOf(errors);
        }
    }
}