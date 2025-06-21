package it.mathsanalysis.load.builder;

import it.mathsanalysis.load.core.DataLoader;
import it.mathsanalysis.load.document.connection.MongoConnectionProvider;
import it.mathsanalysis.load.document.impl.MongoDataLoader;
import it.mathsanalysis.load.document.query.GenericDocumentQueryBuilder;
import it.mathsanalysis.load.document.query.MongoQueryBuilder;
import it.mathsanalysis.load.impl.mapping.ReflectionItemMapper;
import it.mathsanalysis.load.impl.query.ReflectionQueryBuilder;
import it.mathsanalysis.load.impl.serialization.JacksonDocumentSerializer;
import it.mathsanalysis.load.relational.connection.HikariConnectionProvider;
import it.mathsanalysis.load.relational.impl.SqlDataLoader;
import it.mathsanalysis.load.resilience.config.central.DataLoaderConfiguration;
import it.mathsanalysis.load.resilience.event.structure.DataLoaderEventListener;
import it.mathsanalysis.load.spi.cache.factory.CachedDataLoaderFactory;
import it.mathsanalysis.load.spi.connection.ConnectionProvider;
import it.mathsanalysis.load.spi.connection.DocumentConnectionProvider;
import lombok.Getter;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced fluent builder for creating optimized data loaders with type safety and validation.
 * <p>
 * This builder uses a type-safe approach to prevent invalid configurations and provides
 * comprehensive customization options for enterprise-grade applications.
 *
 */
public final class LoaderBuilder<T, ID> {

    // Core configuration
    private final Class<T> itemType;
    private final Class<ID> idType;
    private DataLoaderConfiguration configuration;

    // Database type tracking
    private DatabaseType databaseType;

    // Connection configuration
    private ConnectionProvider connectionProvider;
    private DocumentConnectionProvider documentConnectionProvider;
    private String connectionString;
    private String username;
    private String password;

    // Database configuration
    private String tableName;
    private String collectionName;
    private String databaseName;
    private String schema;
    private String dialect;

    // Enhanced configuration
    private final List<DataLoaderEventListener> eventListeners = new ArrayList<>();
    // Caching configuration
    private boolean enableCaching = false;
    private String cacheName;
    private Duration cacheTimeout = Duration.ofMinutes(15);
    private int cacheMaxSize = 10000;

    // Advanced configuration
    private Map<String, Object> customProperties = new HashMap<>();
    private boolean enableRetries = true;
    private int maxRetries = 3;
    private Duration retryDelay = Duration.ofSeconds(1);
    private boolean enableCircuitBreaker = true;
    private boolean enableMetrics = true;
    private boolean autoInitialize = false;

    // Validation state
    private boolean validated = false;

    /**
     * Database types supported by the framework
     */
    @Getter
    public enum DatabaseType {
        SQL("sql"),
        MONGODB("mongodb"),
        DOCUMENT("document");

        private final String type;

        DatabaseType(String type) {
            this.type = type;
        }
    }

    /**
     * Private constructor - use factory methods
     */
    private LoaderBuilder(Class<T> itemType, Class<ID> idType) {
        this.itemType = Objects.requireNonNull(itemType, "Item type cannot be null");
        this.idType = Objects.requireNonNull(idType, "ID type cannot be null");
        this.configuration = DataLoaderConfiguration.defaults();
    }

    // ===== Factory Methods =====

    /**
     * Start building a loader for the specified types
     *
     * @param itemType The class of items this loader will handle
     * @param idType   The class of item identifiers
     * @return New LoaderBuilder instance
     */
    public static <T, ID> LoaderBuilder<T, ID> forType(Class<T> itemType, Class<ID> idType) {
        return new LoaderBuilder<>(itemType, idType);
    }

    /**
     * Convenience method for String IDs (most common case)
     *
     * @param itemType The class of items this loader will handle
     * @return New LoaderBuilder instance with String ID type
     */
    @SuppressWarnings("unchecked")
    public static <T> LoaderBuilder<T, String> forType(Class<T> itemType) {
        return new LoaderBuilder<>(itemType, (Class<String>) String.class);
    }

    /**
     * Create builder with auto-detected types from sample instance
     *
     * @param sampleItem Sample item to detect types from
     * @param sampleId   Sample ID to detect type from
     * @return New LoaderBuilder instance with detected types
     */
    @SuppressWarnings("unchecked")
    public static <T, ID> LoaderBuilder<T, ID> forInstance(T sampleItem, ID sampleId) {
        Objects.requireNonNull(sampleItem, "Sample item cannot be null");
        Objects.requireNonNull(sampleId, "Sample ID cannot be null");

        Class<T> itemType = (Class<T>) sampleItem.getClass();
        Class<ID> idType = (Class<ID>) sampleId.getClass();

        return new LoaderBuilder<>(itemType, idType);
    }

    // ===== Configuration Methods =====

    /**
     * Configure with pre-built DataLoaderConfiguration
     */
    public LoaderBuilder<T, ID> withConfiguration(DataLoaderConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        return this;
    }

    /**
     * Configure for specific environment with auto-optimization
     */
    public LoaderBuilder<T, ID> forEnvironment(DataLoaderConfiguration.Environment environment) {
        this.configuration = DataLoaderConfiguration.forEnvironment(environment);
        return this;
    }

    // ===== SQL Database Configuration =====

    /**
     * Configure for SQL database with connection string and auto-detection
     */
    public SqlConfigurationBuilder<T, ID> withSqlConnection(String connectionString) {
        this.connectionString = Objects.requireNonNull(connectionString, "Connection string cannot be null");
        this.databaseType = DatabaseType.SQL;
        detectSqlDialect(connectionString);
        return new SqlConfigurationBuilder<>(this);
    }

    /**
     * Configure for SQL database with credentials
     */
    public SqlConfigurationBuilder<T, ID> withSqlConnection(String connectionString, String username, String password) {
        this.connectionString = Objects.requireNonNull(connectionString, "Connection string cannot be null");
        this.username = username;
        this.password = password;
        this.databaseType = DatabaseType.SQL;
        detectSqlDialect(connectionString);
        return new SqlConfigurationBuilder<>(this);
    }

    /**
     * Configure with custom SQL connection provider
     */
    public SqlConfigurationBuilder<T, ID> withSqlConnection(ConnectionProvider provider) {
        this.connectionProvider = Objects.requireNonNull(provider, "Connection provider cannot be null");
        this.databaseType = DatabaseType.SQL;
        return new SqlConfigurationBuilder<>(this);
    }

    /**
     * Configure for MongoDB connection
     */
    public MongoConfigurationBuilder<T, ID> withMongoConnection(String connectionString, String databaseName) {
        this.connectionString = Objects.requireNonNull(connectionString, "Connection string cannot be null");
        this.databaseName = Objects.requireNonNull(databaseName, "Database name cannot be null");
        this.databaseType = DatabaseType.MONGODB;
        return new MongoConfigurationBuilder<>(this);
    }

    /**
     * Configure with custom document connection provider
     */
    public DocumentConfigurationBuilder<T, ID> withDocumentConnection(DocumentConnectionProvider provider) {
        this.documentConnectionProvider = Objects.requireNonNull(provider, "Document connection provider cannot be null");
        this.databaseType = DatabaseType.DOCUMENT;
        return new DocumentConfigurationBuilder<>(this);
    }

    // ===== Caching Configuration =====

    /**
     * Enable caching with default configuration
     */
    public LoaderBuilder<T, ID> withCaching(String cacheName) {
        this.enableCaching = true;
        this.cacheName = Objects.requireNonNull(cacheName, "Cache name cannot be null");
        return this;
    }

    /**
     * Enable caching with custom configuration
     */
    public LoaderBuilder<T, ID> withCaching(String cacheName, Duration timeout, int maxSize) {
        this.enableCaching = true;
        this.cacheName = Objects.requireNonNull(cacheName, "Cache name cannot be null");
        this.cacheTimeout = Objects.requireNonNull(timeout, "Cache timeout cannot be null");
        this.cacheMaxSize = maxSize;
        if (maxSize <= 0) throw new IllegalArgumentException("Cache max size must be positive");
        return this;
    }

    // ===== Plugin and Event Configuration =====

    /**
     * Add event listener for customization
     */
    public LoaderBuilder<T, ID> addEventListener(DataLoaderEventListener listener) {
        this.eventListeners.add(Objects.requireNonNull(listener, "Event listener cannot be null"));
        return this;
    }

    /**
     * Configure retry behavior
     */
    public LoaderBuilder<T, ID> withRetries(int maxRetries, Duration delay) {
        this.enableRetries = true;
        this.maxRetries = maxRetries;
        this.retryDelay = Objects.requireNonNull(delay, "Retry delay cannot be null");
        if (maxRetries < 0) throw new IllegalArgumentException("Max retries cannot be negative");
        return this;
    }

    /**
     * Disable retry behavior
     */
    public LoaderBuilder<T, ID> withoutRetries() {
        this.enableRetries = false;
        return this;
    }

    /**
     * Configure circuit breaker
     */
    public LoaderBuilder<T, ID> withCircuitBreaker(boolean enabled) {
        this.enableCircuitBreaker = enabled;
        return this;
    }

    /**
     * Configure metrics collection
     */
    public LoaderBuilder<T, ID> withMetrics(boolean enabled) {
        this.enableMetrics = enabled;
        return this;
    }

    /**
     * Add custom property
     */
    public LoaderBuilder<T, ID> withProperty(String key, Object value) {
        this.customProperties.put(Objects.requireNonNull(key, "Property key cannot be null"), value);
        return this;
    }

    /**
     * Add multiple custom properties
     */
    public LoaderBuilder<T, ID> withProperties(Map<String, Object> properties) {
        this.customProperties.putAll(Objects.requireNonNull(properties, "Properties cannot be null"));
        return this;
    }

    /**
     * Enable automatic storage initialization
     */
    public LoaderBuilder<T, ID> withAutoInitialization() {
        this.autoInitialize = true;
        return this;
    }

    // ===== Environment-Specific Presets =====

    /**
     * Configure for Minecraft environment with optimizations
     */
    public LoaderBuilder<T, ID> forMinecraft(String pluginName) {
        this.configuration = DataLoaderConfiguration.forEnvironment(
                DataLoaderConfiguration.Environment.MINECRAFT
        );

        // Minecraft-specific optimizations
        withProperty("pluginName", pluginName);
        withProperty("asyncScheduling", true);
        withProperty("mainThreadChecks", true);
        withRetries(2, Duration.ofMillis(100));
        withCircuitBreaker(false); // Keep simple for Minecraft

        return this;
    }

    /**
     * Configure for Spring Boot environment
     */
    public LoaderBuilder<T, ID> forSpringBoot() {
        this.configuration = DataLoaderConfiguration.forEnvironment(
                DataLoaderConfiguration.Environment.PRODUCTION
        );

        // Spring Boot optimizations
        withProperty("springIntegration", true);
        withProperty("transactionManagement", true);

        return this;
    }

    /**
     * Configure for high-performance production environment
     */
    public LoaderBuilder<T, ID> forHighPerformance() {
        this.configuration = DataLoaderConfiguration.productionConfig();

        // High-performance optimizations
        withCircuitBreaker(true);
        withRetries(3, Duration.ofMillis(50));
        withMetrics(true);
        withProperty("connectionPoolOptimized", true);
        withProperty("batchingEnabled", true);

        return this;
    }

    /**
     * Configure for development environment with debugging
     */
    public LoaderBuilder<T, ID> forDevelopment() {
        this.configuration = DataLoaderConfiguration.forEnvironment(
                DataLoaderConfiguration.Environment.DEVELOPMENT
        );

        // Development optimizations
        withMetrics(true);
        withProperty("verboseLogging", true);
        withProperty("debugMode", true);
        withAutoInitialization();

        return this;
    }

    /**
     * Configure for testing environment
     */
    public LoaderBuilder<T, ID> forTesting() {
        this.configuration = DataLoaderConfiguration.forEnvironment(
                DataLoaderConfiguration.Environment.TESTING
        );

        // Testing optimizations
        withoutRetries();
        withCircuitBreaker(false);
        withMetrics(false);
        withProperty("testMode", true);

        return this;
    }

    // ===== Build Methods =====

    /**
     * Build the configured data loader with validation
     */
    public DataLoader<T, ID> build() {
        validateConfiguration();
        applyDefaults();

        var loader = switch (databaseType) {
            case SQL -> buildSqlLoader();
            case MONGODB -> buildMongoLoader();
            case DOCUMENT -> buildDocumentLoader();
            case null -> throw new IllegalStateException("Database type not specified");
        };

        // Apply caching if enabled
        if (enableCaching) {
            return CachedDataLoaderFactory.wrap(loader, cacheName);
        }

        return loader;
    }

    /**
     * Build the loader asynchronously
     */
    public CompletableFuture<DataLoader<T, ID>> buildAsync() {
        return CompletableFuture.supplyAsync(this::build);
    }

    /**
     * Build and initialize storage structure
     */
    public CompletableFuture<DataLoader<T, ID>> buildAndInitialize(Map<String, Object> parameters) {
        return buildAsync()
                .thenCompose(loader -> {
                    if (autoInitialize) {
                        return loader.initializeStorage(parameters)
                                .thenApply(v -> loader);
                    } else {
                        return CompletableFuture.completedFuture(loader);
                    }
                });
    }

    /**
     * Build with validation and return configuration info
     */
    public BuildResult<T, ID> buildWithInfo() {
        var loader = build();
        var config = createFinalConfiguration();
        return new BuildResult<>(loader, config, getValidationResults());
    }

    // ===== Inner Configuration Builders =====

    /**
     * SQL-specific configuration builder
     */
    public static final class SqlConfigurationBuilder<T, ID> {
        private final LoaderBuilder<T, ID> parent;

        private SqlConfigurationBuilder(LoaderBuilder<T, ID> parent) {
            this.parent = parent;
        }

        /**
         * Set table name for SQL databases
         */
        public SqlConfigurationBuilder<T, ID> withTable(String tableName) {
            parent.tableName = Objects.requireNonNull(tableName, "Table name cannot be null");
            return this;
        }

        /**
         * Set database schema for SQL databases
         */
        public SqlConfigurationBuilder<T, ID> withSchema(String schema) {
            parent.schema = schema;
            return this;
        }

        /**
         * Configure connection pool settings
         */
        public SqlConfigurationBuilder<T, ID> withConnectionPool(int maxPoolSize, int minIdle) {
            if (maxPoolSize <= 0) throw new IllegalArgumentException("Max pool size must be positive");
            if (minIdle < 0) throw new IllegalArgumentException("Min idle cannot be negative");
            if (minIdle > maxPoolSize) throw new IllegalArgumentException("Min idle cannot exceed max pool size");

            parent.withProperty("maxPoolSize", maxPoolSize);
            parent.withProperty("minIdle", minIdle);
            return this;
        }

        /**
         * Configure connection pool timeouts
         */
        public SqlConfigurationBuilder<T, ID> withTimeouts(Duration connectionTimeout, Duration idleTimeout) {
            parent.withProperty("connectionTimeout", connectionTimeout);
            parent.withProperty("idleTimeout", idleTimeout);
            return this;
        }

        /**
         * Set SQL dialect explicitly
         */
        public SqlConfigurationBuilder<T, ID> withDialect(String dialect) {
            parent.dialect = Objects.requireNonNull(dialect, "Dialect cannot be null");
            return this;
        }

        // Return to main builder
        public LoaderBuilder<T, ID> and() {
            return parent;
        }

        // Direct build
        public DataLoader<T, ID> build() {
            return parent.build();
        }
    }

    /**
     * MongoDB-specific configuration builder
     */
    public static final class MongoConfigurationBuilder<T, ID> {
        private final LoaderBuilder<T, ID> parent;

        private MongoConfigurationBuilder(LoaderBuilder<T, ID> parent) {
            this.parent = parent;
        }

        /**
         * Set collection name for MongoDB
         */
        public MongoConfigurationBuilder<T, ID> withCollection(String collectionName) {
            parent.collectionName = Objects.requireNonNull(collectionName, "Collection name cannot be null");
            return this;
        }

        /**
         * Configure MongoDB-specific options
         */
        public MongoConfigurationBuilder<T, ID> withMongoOptions(Map<String, Object> options) {
            parent.customProperties.putAll(options);
            return this;
        }

        /**
         * Configure write concern
         */
        public MongoConfigurationBuilder<T, ID> withWriteConcern(String writeConcern) {
            parent.withProperty("writeConcern", writeConcern);
            return this;
        }

        /**
         * Configure read preference
         */
        public MongoConfigurationBuilder<T, ID> withReadPreference(String readPreference) {
            parent.withProperty("readPreference", readPreference);
            return this;
        }

        // Return to main builder
        public LoaderBuilder<T, ID> and() {
            return parent;
        }

        // Direct build
        public DataLoader<T, ID> build() {
            return parent.build();
        }
    }

    /**
     * Document database configuration builder
     */
    public static final class DocumentConfigurationBuilder<T, ID> {
        private final LoaderBuilder<T, ID> parent;

        private DocumentConfigurationBuilder(LoaderBuilder<T, ID> parent) {
            this.parent = parent;
        }

        /**
         * Set collection name for document databases
         */
        public DocumentConfigurationBuilder<T, ID> withCollection(String collectionName) {
            parent.collectionName = Objects.requireNonNull(collectionName, "Collection name cannot be null");
            return this;
        }

        /**
         * Set database name for document databases
         */
        public DocumentConfigurationBuilder<T, ID> withDatabase(String databaseName) {
            parent.databaseName = Objects.requireNonNull(databaseName, "Database name cannot be null");
            return this;
        }

        /**
         * Configure document-specific options
         */
        public DocumentConfigurationBuilder<T, ID> withDocumentOptions(Map<String, Object> options) {
            parent.customProperties.putAll(options);
            return this;
        }

        // Return to main builder
        public LoaderBuilder<T, ID> and() {
            return parent;
        }

        // Direct build
        public DataLoader<T, ID> build() {
            return parent.build();
        }
    }

    // ===== Private Implementation Methods =====

    private void validateConfiguration() {
        if (validated) return;

        var errors = new ArrayList<String>();

        // Validate database type
        if (databaseType == null) {
            errors.add("Database type not specified. Use withSqlConnection(), withMongoConnection(), or withDocumentConnection()");
        }

        // Validate database-specific configuration
        if (databaseType != null) {
            switch (databaseType) {
                case SQL -> validateSqlConfiguration(errors);
                case MONGODB -> validateMongoConfiguration(errors);
                case DOCUMENT -> validateDocumentConfiguration(errors);
            }
        }

        // Validate caching configuration
        if (enableCaching && cacheName == null) {
            errors.add("Cache name must be specified when caching is enabled");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Configuration validation failed: " + String.join(", ", errors));
        }

        validated = true;
    }

    private void validateSqlConfiguration(List<String> errors) {
        if (connectionProvider == null && connectionString == null) {
            errors.add("SQL connection string or connection provider must be specified");
        }
        if (tableName == null) {
            // Will be auto-generated in applyDefaults()
        }
    }

    private void validateMongoConfiguration(List<String> errors) {
        if (documentConnectionProvider == null && connectionString == null) {
            errors.add("MongoDB connection string or document connection provider must be specified");
        }
        if (databaseName == null) {
            errors.add("MongoDB database name must be specified");
        }
        if (collectionName == null) {
            // Will be auto-generated in applyDefaults()
        }
    }

    private void validateDocumentConfiguration(List<String> errors) {
        if (documentConnectionProvider == null) {
            errors.add("Document connection provider must be specified");
        }
        if (collectionName == null) {
            // Will be auto-generated in applyDefaults()
        }
    }

    private void applyDefaults() {
        // Apply default table/collection names if not specified
        if (tableName == null && databaseType == DatabaseType.SQL) {
            tableName = generateDefaultTableName();
        }
        if (collectionName == null && (databaseType == DatabaseType.MONGODB || databaseType == DatabaseType.DOCUMENT)) {
            collectionName = generateDefaultCollectionName();
        }

        // Apply default cache name if caching enabled but no name specified
        if (enableCaching && cacheName == null) {
            cacheName = itemType.getSimpleName().toLowerCase() + "-cache";
        }
    }

    private DataLoader<T, ID> buildSqlLoader() {
        // Create or use existing connection provider
        if (connectionProvider == null) {
            connectionProvider = createSqlConnectionProvider();
        }

        // Create SQL-specific components
        var queryBuilder = new ReflectionQueryBuilder<>(itemType, tableName, enableCaching);
        var itemMapper = new ReflectionItemMapper<>(itemType);

        // Build the loader - use correct constructor signature
        var loader = new SqlDataLoader<>(
                itemType,
                idType,
                connectionProvider,
                queryBuilder,
                itemMapper,
                tableName,
                configurationToMap()
        );

        // Configure plugins and event listeners
        configureLoaderExtensions(loader);

        return loader;
    }

    private DataLoader<T, ID> buildMongoLoader() {
        // Create or use existing document connection provider
        if (documentConnectionProvider == null) {
            documentConnectionProvider = new MongoConnectionProvider(
                    connectionString,
                    databaseName
            );
        }

        // Create MongoDB-specific components
        var serializer = new JacksonDocumentSerializer<T>();
        var queryBuilder = new MongoQueryBuilder<T>();

        // Build the loader
        var loader = new MongoDataLoader<>(
                itemType,
                idType,
                documentConnectionProvider,
                serializer,
                queryBuilder,
                collectionName,
                databaseName,
                configurationToMap()
        );

        // Configure plugins and event listeners
        configureLoaderExtensions(loader);

        return loader;
    }

    private DataLoader<T, ID> buildDocumentLoader() {
        // Generic document loader implementation
        var serializer = new JacksonDocumentSerializer<T>();

        // Use GenericDocumentQueryBuilder for generic document databases
        var queryBuilder = new GenericDocumentQueryBuilder<T>(itemType);

        var loader = new MongoDataLoader<>(
                itemType,
                idType,
                documentConnectionProvider,
                serializer,
                queryBuilder,
                collectionName,
                databaseName != null ? databaseName : "default",
                configurationToMap()
        );

        configureLoaderExtensions(loader);
        return loader;
    }

    private void configureLoaderExtensions(DataLoader<T, ID> loader) {
        // Configure plugins and event listeners if the loader is an AbstractDataLoader
        if (loader instanceof it.mathsanalysis.load.core.AbstractDataLoader<T, ID> abstractLoader) {
            eventListeners.forEach(abstractLoader::addEventListener);
        }
        // Note: If using wrapper classes or other implementations,
        // additional configuration logic can be added here
    }

    private ConnectionProvider createSqlConnectionProvider() {
        // Use safe defaults if configuration is not available
        var maxPoolSize = getConfigValue("maxPoolSize", 20);
        var minIdle = getConfigValue("minIdle", 5);
        var connectionTimeout = getConfigValue("connectionTimeout", Duration.ofSeconds(30));
        var idleTimeout = getConfigValue("idleTimeout", Duration.ofMinutes(10));
        var maxLifetime = getConfigValue("maxLifetime", Duration.ofMinutes(30));

        return new HikariConnectionProvider(
                connectionString,
                username,
                password,
                maxPoolSize,
                minIdle,
                connectionTimeout.toMillis(),
                idleTimeout.toMillis(),
                maxLifetime.toMillis()
        );
    }

    private void detectSqlDialect(String connectionString) {
        var lowerUrl = connectionString.toLowerCase();

        if (lowerUrl.contains("postgresql")) {
            dialect = "postgresql";
        } else if (lowerUrl.contains("mysql")) {
            dialect = "mysql";
        } else if (lowerUrl.contains("h2")) {
            dialect = "h2";
        } else if (lowerUrl.contains("sqlite")) {
            dialect = "sqlite";
        } else if (lowerUrl.contains("oracle")) {
            dialect = "oracle";
        } else if (lowerUrl.contains("sqlserver")) {
            dialect = "sqlserver";
        } else {
            dialect = "generic";
        }
    }

    private String generateDefaultTableName() {
        return itemType.getSimpleName().toLowerCase() + "s";
    }

    private String generateDefaultCollectionName() {
        return itemType.getSimpleName().toLowerCase() + "s";
    }

    private Map<String, Object> configurationToMap() {
        var map = new HashMap<String, Object>();

        // Add framework configuration
        map.put("itemType", itemType);
        map.put("idType", idType);
        map.put("databaseType", databaseType);
        map.put("enableMetrics", enableMetrics);
        map.put("enableRetries", enableRetries);
        map.put("maxRetries", maxRetries);
        map.put("retryDelay", retryDelay);
        map.put("enableCircuitBreaker", enableCircuitBreaker);
        map.put("autoInitialize", autoInitialize);

        // Add database-specific configuration
        if (dialect != null) {
            map.put("dialect", dialect);
        }
        if (schema != null) {
            map.put("schema", schema);
        }

        // Add caching configuration
        if (enableCaching) {
            map.put("enableCache", true);
            map.put("cacheName", cacheName);
            map.put("cacheTimeout", cacheTimeout);
            map.put("cacheMaxSize", cacheMaxSize);
        }

        // Add DataLoaderConfiguration values if available
        if (configuration != null) {
            try {
                var conn = configuration.getConnection();
                map.put("maxPoolSize", conn.maxPoolSize());
                map.put("minIdle", conn.minIdle());
                map.put("connectionTimeoutMs", conn.connectionTimeout().toMillis());
                map.put("idleTimeoutMs", conn.idleTimeout().toMillis());
                map.put("maxLifetimeMs", conn.maxLifetime().toMillis());

                var cache = configuration.getCache();
                if (!enableCaching) { // Don't override explicit caching config
                    map.put("enableCache", cache.enabled());
                    map.put("cacheMaxSize", cache.maxSize());
                    map.put("cacheTtl", cache.ttl().toSeconds());
                }

                var metrics = configuration.getMetrics();
                map.put("enableMetrics", metrics.enabled());
                map.put("detailedMetrics", metrics.detailedMetrics());

                var security = configuration.getSecurity();
                map.put("encryptionEnabled", security.encryptionEnabled());
                map.put("auditLogging", security.auditLogging());

                var resilience = configuration.getResilience();
                map.put("circuitBreakerEnabled", resilience.circuitBreakerEnabled());
                map.put("retryEnabled", resilience.retryEnabled());
                map.put("maxRetries", resilience.maxRetries());
            } catch (Exception e) {
                // If configuration access fails, continue with custom properties only
                System.err.println("Warning: Could not access configuration properties: " + e.getMessage());
            }
        }

        // Add custom properties (these override defaults)
        map.putAll(customProperties);

        return map;
    }

    private Map<String, Object> createFinalConfiguration() {
        return Map.copyOf(configurationToMap());
    }

    private List<String> getValidationResults() {
        // Return any validation warnings or info
        var results = new ArrayList<String>();

        if (tableName != null && tableName.equals(generateDefaultTableName())) {
            results.add("Using auto-generated table name: " + tableName);
        }

        if (collectionName != null && collectionName.equals(generateDefaultCollectionName())) {
            results.add("Using auto-generated collection name: " + collectionName);
        }

        if (enableCaching) {
            results.add("Caching enabled with name: " + cacheName);
        }

        if (!eventListeners.isEmpty()) {
            results.add("Registered " + eventListeners.size() + " event listeners");
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private <V> V getConfigValue(String key, V defaultValue) {
        var value = customProperties.get(key);
        if (defaultValue.getClass().isInstance(value)) {
            return (V) value;
        }
        return defaultValue;
    }


    public record BuildResult<T, ID>(DataLoader<T, ID> loader, Map<String, Object> configuration, List<String> validationResults) {

        public void printValidationResults() {
            validationResults.forEach(System.out::println);
        }
    }
}