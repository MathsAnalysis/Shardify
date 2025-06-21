package it.mathsanalysis.load.plugin.core;

import it.mathsanalysis.load.annotations.impl.NotNull;
import it.mathsanalysis.load.annotations.impl.Size;
import it.mathsanalysis.load.plugin.structure.PluginContext;
import it.mathsanalysis.load.plugin.structure.ValidationPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public final class AnnotationValidationPlugin extends ValidationPlugin {

    @Override
    public String getName() {
        return "AnnotationValidation";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void initialize(PluginContext context) {
        // init if you need
    }

    @Override
    public void shutdown() {
        // cleanup if you need
    }

    @Override
    protected <T> List<String> validate(T item) {
        List<String> errors = new ArrayList<>();

        for (Field field : item.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(item);

                // @NotNull validation
                if (field.isAnnotationPresent(NotNull.class) && value == null) {
                    var annotation = field.getAnnotation(NotNull.class);
                    errors.add(annotation.message() + " (field: " + field.getName() + ")");
                }

                // @Size validation
                if (field.isAnnotationPresent(Size.class) && value instanceof String str) {
                    var annotation = field.getAnnotation(Size.class);
                    if (str.length() < annotation.min() || str.length() > annotation.max()) {
                        errors.add(annotation.message() + " (field: " + field.getName() + ")");
                    }
                }

            } catch (IllegalAccessException e) {
                errors.add("Cannot access field: " + field.getName());
            }
        }

        return errors;
    }
}
