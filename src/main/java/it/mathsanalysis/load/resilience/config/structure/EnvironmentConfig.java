package it.mathsanalysis.load.resilience.config.structure;

import java.util.Map;

public record EnvironmentConfig(
        String name,
        Map<String, Object> properties
) {
    public static EnvironmentConfig of(String name, Map<String, Object> properties) {
        return new EnvironmentConfig(name, Map.copyOf(properties));
    }
}
