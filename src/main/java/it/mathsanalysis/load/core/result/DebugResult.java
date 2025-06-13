package it.mathsanalysis.load.core.result;

import java.util.Map;

/**
 * Debug information container
 * Comprehensive debugging data for troubleshooting
 */
public record DebugResult(
    String loaderType,
    Map<String, Object> performanceStats,
    Map<String, Object> connectionStats,
    Map<String, Object> additionalInfo
) {
    
    /**
     * Format debug info as human-readable string
     * @return Formatted debug information
     */
    public String getFormattedInfo() {
        var sb = new StringBuilder();
        sb.append("=== ").append(loaderType).append(" Debug Info ===\n");
        sb.append("Performance Stats:\n");
        performanceStats.forEach((key, value) -> sb.append("  ").append(key).append(": ").append(value).append("\n"));
        sb.append("Connection Stats:\n");
        connectionStats.forEach((key, value) -> sb.append("  ").append(key).append(": ").append(value).append("\n"));
        sb.append("Additional Info:\n");
        additionalInfo.forEach((key, value) -> sb.append("  ").append(key).append(": ").append(value).append("\n"));
        return sb.toString();
    }
}