package it.mathsanalysis.load.core.result;

import java.util.List;

/**
 * Result container for batch operations
 * Provides success/failure information and performance data
 */
public record BatchResult<T>(
    List<T> successfulItems,
    int totalProcessed,
    List<Exception> errors
) {
    
    /**
     * Check if batch operation was completely successful
     * @return true if no errors occurred
     */
    public boolean isFullySuccessful() {
        return errors.isEmpty();
    }
    
    /**
     * Get success rate as percentage
     * @return Success rate (0.0 to 1.0)
     */
    public double getSuccessRate() {
        return totalProcessed > 0 ? (double) successfulItems.size() / totalProcessed : 0.0;
    }
}