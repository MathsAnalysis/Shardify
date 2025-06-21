package it.mathsanalysis.load.resilience.config.structure;

import java.time.Duration;

public record CacheConfig(
        boolean enabled,
        long maxSize,
        Duration ttl,
        Duration expireAfterAccess,
        boolean recordStats
) {
    public static CacheConfig.Builder builder() {
        return new CacheConfig.Builder();
    }

    public static CacheConfig defaults() {
        return builder().build();
    }

    public static class Builder {
        private boolean enabled = true;
        private long maxSize = 10000;
        private Duration ttl = Duration.ofMinutes(30);
        private Duration expireAfterAccess = Duration.ofMinutes(15);
        private boolean recordStats = true;

        public CacheConfig.Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public CacheConfig.Builder maxSize(long maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public CacheConfig.Builder ttl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        public CacheConfig.Builder expireAfterAccess(Duration expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
            return this;
        }

        public CacheConfig.Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        public CacheConfig build() {
            return new CacheConfig(enabled, maxSize, ttl, expireAfterAccess, recordStats);
        }
    }
}
