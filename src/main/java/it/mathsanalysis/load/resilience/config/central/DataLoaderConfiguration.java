package it.mathsanalysis.load.resilience.config.central;

import it.mathsanalysis.load.resilience.config.structure.*;
import lombok.Getter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized configuration for DataLoader framework
 * Supports environment-specific settings and validation
 */
@Getter
public final class DataLoaderConfiguration {

    private final ConnectionConfig connection;
    private final CacheConfig cache;
    private final MetricsConfig metrics;
    private final SecurityConfig security;
    private final ResilienceConfig resilience;
    private final Map<String, EnvironmentConfig> environments;
    
    private DataLoaderConfiguration(Builder builder) {
        this.connection = builder.connection;
        this.cache = builder.cache;
        this.metrics = builder.metrics;
        this.security = builder.security;
        this.resilience = builder.resilience;
        this.environments = Map.copyOf(builder.environments);
        
        validate();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .connection(connection)
            .cache(cache)
            .metrics(metrics)
            .security(security)
            .resilience(resilience);
    }

    
    public static DataLoaderConfiguration defaults() {
        return builder().build();
    }
    
    public static DataLoaderConfiguration forEnvironment(Environment env) {
        return switch (env) {
            case DEVELOPMENT -> developmentConfig();
            case TESTING -> testingConfig();
            case PRODUCTION -> productionConfig();
            case MINECRAFT -> minecraftConfig();
        };
    }
    
    private static DataLoaderConfiguration developmentConfig() {
        return builder()
            .connection(ConnectionConfig.builder()
                .maxPoolSize(5)
                .minIdle(1)
                .connectionTimeout(Duration.ofSeconds(10))
                .build())
            .cache(CacheConfig.builder()
                .enabled(true)
                .maxSize(1000)
                .ttl(Duration.ofMinutes(5))
                .build())
            .metrics(MetricsConfig.builder()
                .enabled(true)
                .detailedMetrics(true)
                .build())
            .build();
    }
    
    private static DataLoaderConfiguration testingConfig() {
        return builder()
            .connection(ConnectionConfig.builder()
                .maxPoolSize(2)
                .minIdle(0)
                .connectionTimeout(Duration.ofSeconds(5))
                .build())
            .cache(CacheConfig.builder()
                .enabled(false)
                .build())
            .metrics(MetricsConfig.builder()
                .enabled(false)
                .build())
            .build();
    }
    
    public static DataLoaderConfiguration productionConfig() {
        return builder()
            .connection(ConnectionConfig.builder()
                .maxPoolSize(100)
                .minIdle(10)
                .connectionTimeout(Duration.ofSeconds(30))
                .idleTimeout(Duration.ofMinutes(10))
                .maxLifetime(Duration.ofMinutes(30))
                .build())
            .cache(CacheConfig.builder()
                .enabled(true)
                .maxSize(50000)
                .ttl(Duration.ofHours(1))
                .build())
            .metrics(MetricsConfig.builder()
                .enabled(true)
                .detailedMetrics(true)
                .exportInterval(Duration.ofMinutes(1))
                .build())
            .security(SecurityConfig.builder()
                .encryptionEnabled(true)
                .auditLogging(true)
                .build())
            .resilience(ResilienceConfig.builder()
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .maxRetries(3)
                .build())
            .build();
    }
    
    private static DataLoaderConfiguration minecraftConfig() {
        return builder()
            .connection(ConnectionConfig.builder()
                .maxPoolSize(5)
                .minIdle(1)
                .connectionTimeout(Duration.ofSeconds(5))
                .build())
            .cache(CacheConfig.builder()
                .enabled(true)
                .maxSize(1000)
                .ttl(Duration.ofMinutes(10))
                .build())
            .metrics(MetricsConfig.builder()
                .enabled(true)
                .detailedMetrics(false)
                .lightweightMode(true)
                .build())
            .resilience(ResilienceConfig.builder()
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .maxRetries(2)
                .build())
            .build();
    }
    
    private void validate() {
        if (connection.maxPoolSize() <= 0) {
            throw new IllegalArgumentException("Max pool size must be positive");
        }
        if (connection.minIdle() < 0 || connection.minIdle() > connection.maxPoolSize()) {
            throw new IllegalArgumentException("Min idle must be between 0 and max pool size");
        }
        if (cache.enabled() && cache.maxSize() <= 0) {
            throw new IllegalArgumentException("Cache max size must be positive when cache is enabled");
        }
    }

    public enum Environment {
        DEVELOPMENT, TESTING, PRODUCTION, MINECRAFT
    }
    
    // Builder Pattern
    public static class Builder {
        private ConnectionConfig connection = ConnectionConfig.defaults();
        private CacheConfig cache = CacheConfig.defaults();
        private MetricsConfig metrics = MetricsConfig.defaults();
        private SecurityConfig security = SecurityConfig.defaults();
        private ResilienceConfig resilience = ResilienceConfig.defaults();
        private final Map<String, EnvironmentConfig> environments = new HashMap<>();
        
        public Builder connection(ConnectionConfig connection) {
            this.connection = connection;
            return this;
        }
        
        public Builder cache(CacheConfig cache) {
            this.cache = cache;
            return this;
        }
        
        public Builder metrics(MetricsConfig metrics) {
            this.metrics = metrics;
            return this;
        }
        
        public Builder security(SecurityConfig security) {
            this.security = security;
            return this;
        }
        
        public Builder resilience(ResilienceConfig resilience) {
            this.resilience = resilience;
            return this;
        }
        
        public Builder environment(String name, EnvironmentConfig config) {
            this.environments.put(name, config);
            return this;
        }
        
        public DataLoaderConfiguration build() {
            return new DataLoaderConfiguration(this);
        }
    }
}


