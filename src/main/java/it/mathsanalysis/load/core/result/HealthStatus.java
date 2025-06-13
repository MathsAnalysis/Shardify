package it.mathsanalysis.load.core.result;

import java.util.Map;
import java.util.Objects;

/**
 * Health status information
 * System health and connectivity status
 */
public record HealthStatus(
    boolean isHealthy,
    String message,
    Map<String, Object> metrics
) {
    
    /**
     * Create healthy status
     * @param metrics Performance metrics
     * @return Healthy status instance
     */
    public static HealthStatus healthy(Map<String, Object> metrics, String message) {
        return new HealthStatus(true, message, metrics);
    }

    /**
     * Create unhealthy status
     * @param reason Reason for unhealthy status
     * @param metrics Performance metrics
     * @return Unhealthy status instance
     */
    public static HealthStatus unhealthy(String reason, Map<String, Object> metrics) {
        return new HealthStatus(false, reason, metrics);
    }
}