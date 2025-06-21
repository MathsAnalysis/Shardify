// ===== src/main/java/it/mathsanalysis/load/event/DataLoaderEvent.java =====
package it.mathsanalysis.load.resilience.event.structure;

import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Base class per tutti gli eventi del DataLoader
 */
@Getter
public abstract class DataLoaderEvent<T> {
    
    private final T item;
    private final String operationType;
    private final Instant timestamp;
    private final Map<String, Object> context;
    
    protected DataLoaderEvent(T item, String operationType, Map<String, Object> context) {
        this.item = item;
        this.operationType = operationType;
        this.timestamp = Instant.now();
        this.context = context != null ? Map.copyOf(context) : Map.of();
    }

}