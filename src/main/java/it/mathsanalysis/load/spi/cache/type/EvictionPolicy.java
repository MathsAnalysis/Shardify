
package it.mathsanalysis.load.spi.cache.type;

/**
 * Cache eviction policies
 */
public enum EvictionPolicy {
    LRU,     // Least Recently Used
    LFU,     // Least Frequently Used  
    FIFO,    // First In First Out
    RANDOM,  // Random eviction
    NONE     // No automatic eviction
}