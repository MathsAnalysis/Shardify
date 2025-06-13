package it.mathsanalysis.load.spi.cache.structure;

import it.mathsanalysis.load.spi.cache.type.EvictionPolicy;

import java.time.Duration;

/**
 * Cache configuration container
 */
public record CacheConfiguration(
    String name,
    long maxSize,
    Duration defaultTtl,
    Duration maxIdleDuration,
    boolean recordStats,
    boolean allowNullValues,
    EvictionPolicy evictionPolicy,
    int concurrencyLevel,
    Duration expireAfterWrite,
    Duration expireAfterAccess,
    Duration refreshAfterWrite,
    boolean weakKeys,
    boolean weakValues,
    boolean softValues
) {
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String name = "default";
        private long maxSize = 1000;
        private Duration defaultTtl = Duration.ofHours(1);
        private Duration maxIdleDuration = Duration.ofMinutes(30);
        private boolean recordStats = true;
        private boolean allowNullValues = false;
        private EvictionPolicy evictionPolicy = EvictionPolicy.LRU;
        private int concurrencyLevel = 16;
        private Duration expireAfterWrite = null;
        private Duration expireAfterAccess = null;
        private Duration refreshAfterWrite = null;
        private boolean weakKeys = false;
        private boolean weakValues = false;
        private boolean softValues = false;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder maxSize(long maxSize) {
            this.maxSize = maxSize;
            return this;
        }
        
        public Builder defaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
            return this;
        }
        
        public Builder maxIdleDuration(Duration maxIdleDuration) {
            this.maxIdleDuration = maxIdleDuration;
            return this;
        }
        
        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }
        
        public Builder allowNullValues(boolean allowNullValues) {
            this.allowNullValues = allowNullValues;
            return this;
        }
        
        public Builder evictionPolicy(EvictionPolicy evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
            return this;
        }
        
        public Builder concurrencyLevel(int concurrencyLevel) {
            this.concurrencyLevel = concurrencyLevel;
            return this;
        }
        
        public Builder expireAfterWrite(Duration expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
            return this;
        }
        
        public Builder expireAfterAccess(Duration expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
            return this;
        }
        
        public Builder refreshAfterWrite(Duration refreshAfterWrite) {
            this.refreshAfterWrite = refreshAfterWrite;
            return this;
        }
        
        public Builder weakKeys(boolean weakKeys) {
            this.weakKeys = weakKeys;
            return this;
        }
        
        public Builder weakValues(boolean weakValues) {
            this.weakValues = weakValues;
            return this;
        }
        
        public Builder softValues(boolean softValues) {
            this.softValues = softValues;
            return this;
        }
        
        public CacheConfiguration build() {
            return new CacheConfiguration(
                name, maxSize, defaultTtl, maxIdleDuration, recordStats,
                allowNullValues, evictionPolicy, concurrencyLevel,
                expireAfterWrite, expireAfterAccess, refreshAfterWrite,
                weakKeys, weakValues, softValues
            );
        }
    }
}