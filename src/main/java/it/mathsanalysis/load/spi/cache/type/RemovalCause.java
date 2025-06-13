package it.mathsanalysis.load.spi.cache.type;

/**
 * Removal cause enumeration
 */
public enum RemovalCause {
    EXPLICIT,    // Manually removed
    REPLACED,    // Value was replaced
    COLLECTED,   // Garbage collected
    EXPIRED,     // TTL expired
    SIZE,        // Evicted due to size limit
    UNKNOWN      // Unknown cause
}
