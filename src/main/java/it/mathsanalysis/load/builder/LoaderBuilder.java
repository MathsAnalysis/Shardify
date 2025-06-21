// ===== src/main/java/it/mathsanalysis/load/builder/LoaderBuilder.java =====
package it.mathsanalysis.load.builder;

import it.mathsanalysis.load.core.DataLoader;
import it.mathsanalysis.load.plugin.core.AnnotationValidationPlugin;
import it.mathsanalysis.load.plugin.core.LoggingPlugin;
import it.mathsanalysis.load.plugin.core.MetricsPlugin;
import it.mathsanalysis.load.plugin.structure.DataLoaderPlugin;
import it.mathsanalysis.load.relational.impl.SqlDataLoader;
import it.mathsanalysis.load.document.impl.MongoDataLoader;
import it.mathsanalysis.load.relational.connection.HikariConnectionProvider;
import it.mathsanalysis.load.document.connection.MongoConnectionProvider;
import it.mathsanalysis.load.impl.query.ReflectionQueryBuilder;
import it.mathsanalysis.load.document.query.MongoQueryBuilder;
import it.mathsanalysis.load.impl.mapping.ReflectionItemMapper;
import it.mathsanalysis.load.impl.serialization.JacksonDocumentSerializer;
import it.mathsanalysis.load.resilience.config.central.DataLoaderConfiguration;
import it.mathsanalysis.load.resilience.event.structure.DataLoaderEventListener;
import it.mathsanalysis.load.spi.connection.ConnectionProvider;
import it.mathsanalysis.load.spi.connection.DocumentConnectionProvider;
import it.mathsanalysis.load.spi.query.DocumentQueryBuilder;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced fluent builder for creating optimized data loaders with type safety and validation.
 *
 * This builder uses a type-safe approach to prevent invalid configurations and provides
 * comprehensive customization options for enterprise-grade applications.
 *
 * Features:
 * - Type-safe configuration flow
 * - Environment-specific presets
 * - Plugin system integration
 * - Event listener support
 * - Comprehensive validation
 * - Auto-configuration capabilities
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

    // Enhanced configuration
    private final List<DataLoaderEventListener> eventListeners = new ArrayList<>();
    private final List<DataLoaderPlugin> plugins = new ArrayList<>();

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
     */
    public static <T, ID> LoaderBuilder<T, ID> forType(Class<T> itemType, Class<ID> idType) {
        return new LoaderBuilder<>(itemType, idType);
    }

    /**
     * Convenience method for String IDs (most common case)
     */
    @SuppressWarnings("unchecked")
    public static <T> LoaderBuilder<T, String> forType(Class<T> itemType) {
        return new LoaderBuilder<>(itemType, (Class<String>) String.class);
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
     * Configure for SQL database with connection string
     * Returns a type-safe SQL configuration builder
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
     * Returns a type-safe MongoDB configuration builder
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


    /**
     * Add event listener for customization
     */
    public LoaderBuilder<T, ID> addEventListener(DataLoaderEventListener listener) {
        this.eventListeners.add(Objects.requireNonNull(listener, "Event listener cannot be null"));
        return this;
    }

    /**
     * Add plugin for extensibility
     */
    public LoaderBuilder<T, ID> addPlugin(DataLoaderPlugin plugin) {
        this.plugins.add(Objects.requireNonNull(plugin, "Plugin cannot be null"));
        return this;
    }

    /**
     * Add standard plugins for common functionality
     */
    public LoaderBuilder<T, ID> withStandardPlugins() {
        addPlugin(new AnnotationValidationPlugin());
        addPlugin(new LoggingPlugin());
        addPlugin(new MetricsPlugin());
        return this;
    }


    /**
     * Configure for Minecraft environment with optimizations
     */
    public LoaderBuilder<T, ID> forMinecraft(String pluginName) {
        this.configuration = DataLoaderConfiguration.forEnvironment(
                DataLoaderConfiguration.Environment.MINECRAFT
        );

        return this;
    }

    /**
     * Configure for Spring Boot environment
     */
    public LoaderBuilder<T, ID> forSpringBoot() {
        this.configuration = DataLoaderConfiguration.forEnvironment(
                DataLoaderConfiguration.Environment.PRODUCTION
        );
        return this;
    }

    /**
     * Configure for high-performance production environment
     */
    public LoaderBuilder<T, ID> forHighPerformance() {
        this.configuration = DataLoaderConfiguration.productionConfig();
        return withStandardPlugins();
    }


    /**
     * Build the configured data loader with validation
     */
    public DataLoader<T, ID> build() {
        validateConfiguration();
        applyDefaults();

        return switch (databaseType) {
            case SQL -> buildSqlLoader();
            case MONGODB -> buildMongoLoader();
            case DOCUMENT -> buildDocumentLoader();
            case null -> throw new IllegalStateException("Database type not specified");
        };
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
                .thenCompose(loader ->
                        loader.initializeStorage(parameters)
                                .thenApply(v -> loader)
                );
    }

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

//            var connectionConfig = parent.configuration.getConnection()
//                    .maxPoolSize(maxPoolSize)
//                    .minIdle(minIdle)
//                    .build();

//            parent.configuration = parent.configuration.toBuilder()
//                    .connection(connectionConfig)
//                    .build();

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
            // Store MongoDB-specific configuration
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

        // Return to main builder
        public LoaderBuilder<T, ID> and() {
            return parent;
        }

        // Direct build
        public DataLoader<T, ID> build() {
            return parent.build();
        }
    }

    private void validateConfiguration() {
        if (validated) return;

        if (databaseType == null) {
            throw new IllegalStateException("Database type not specified. Use withSqlConnection() or withMongoConnection()");
        }

        switch (databaseType) {
            case SQL -> {
                if (connectionProvider == null && connectionString == null) {
                    throw new IllegalStateException("SQL connection not configured");
                }
            }
            case MONGODB, DOCUMENT -> {
                if (documentConnectionProvider == null && connectionString == null) {
                    throw new IllegalStateException("Document connection not configured");
                }
                if (databaseType == DatabaseType.MONGODB && databaseName == null) {
                    throw new IllegalStateException("Database name required for MongoDB");
                }
            }
        }

        validated = true;
    }

    private void applyDefaults() {
        if (tableName == null && databaseType == DatabaseType.SQL) {
            tableName = generateDefaultTableName();
        }

        if (collectionName == null && (databaseType == DatabaseType.MONGODB || databaseType == DatabaseType.DOCUMENT)) {
            collectionName = generateDefaultCollectionName();
        }
    }

    private DataLoader<T, ID> buildSqlLoader() {
        // Create connection provider if not provided
        if (connectionProvider == null) {
            connectionProvider = createSqlConnectionProvider();
        }

        // Create SQL-specific components
        var queryBuilder = new ReflectionQueryBuilder<T>(itemType, tableName,
                configuration.getCache().enabled());
        var itemMapper = new ReflectionItemMapper<>(itemType);

        // Build the loader
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
        // Create connection provider if not provided
        if (documentConnectionProvider == null) {
            documentConnectionProvider = new MongoConnectionProvider(connectionString, databaseName);
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
        var queryBuilder = new GenericDocumentQueryBuilder<T>();

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
        // This would require access to the internal components of the loader
        // In a real implementation, you'd need to expose plugin and event registration methods
        // on the DataLoader interface or AbstractDataLoader

        // For now, this is a placeholder showing the intent
        // eventListeners.forEach(listener -> loader.addEventListener(listener));
        // plugins.forEach(plugin -> loader.addPlugin(plugin));
    }

    private ConnectionProvider createSqlConnectionProvider() {
        var connConfig = configuration.getConnection();
        return new HikariConnectionProvider(
                connectionString,
                username,
                password,
                connConfig.maxPoolSize(),
                connConfig.minIdle(),
                connConfig.connectionTimeout().toMillis(),
                connConfig.idleTimeout().toMillis(),
                connConfig.maxLifetime().toMillis()
        );
    }

    private void detectSqlDialect(String connectionString) {
        var dialect = "generic";
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
        }

        // Store dialect information for later use
    }

    private String generateDefaultTableName() {
        return itemType.getSimpleName().toLowerCase() + "s";
    }

    private String generateDefaultCollectionName() {
        return itemType.getSimpleName().toLowerCase() + "s";
    }

    private Map<String, Object> configurationToMap() {
        // Convert DataLoaderConfiguration to Map for compatibility with existing code
        var map = new HashMap<String, Object>();

        var conn = configuration.getConnection();
        map.put("maxPoolSize", conn.maxPoolSize());
        map.put("minIdle", conn.minIdle());
        map.put("connectionTimeoutMs", conn.connectionTimeout().toMillis());
        map.put("idleTimeoutMs", conn.idleTimeout().toMillis());
        map.put("maxLifetimeMs", conn.maxLifetime().toMillis());

        var cache = configuration.getCache();
        map.put("enableCache", cache.enabled());
        map.put("cacheMaxSize", cache.maxSize());
        map.put("cacheTtl", cache.ttl().toSeconds());

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

        return map;
    }

    // Generic document query builder for non-MongoDB document databases
    private static class GenericDocumentQueryBuilder<T> implements DocumentQueryBuilder<T> {

        private final Map<String, Object> queryStats = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public Object buildFindByIdQuery(Object id) {
            recordQuery("findById");
            return Map.of("_id", id);
        }

        @Override
        public Object buildInsertQuery(T item, Map<String, Object> parameters) {
            recordQuery("insert");
            return Map.of("operation", "insert", "parameters", parameters);
        }

        @Override
        public Object buildUpdateQuery(T item, Map<String, Object> parameters) {
            recordQuery("update");
            return Map.of("operation", "update", "parameters", parameters);
        }

        @Override
        public Object buildDeleteQuery(T item, Map<String, Object> parameters) {
            recordQuery("delete");
            return Map.of("operation", "delete", "parameters", parameters);
        }

        @Override
        public Object buildFindQuery(Map<String, Object> criteria, Map<String, Object> parameters) {
            recordQuery("find");
            return criteria;
        }

        @Override
        public Object buildAggregationQuery(java.util.List<Map<String, Object>> pipeline, Map<String, Object> parameters) {
            recordQuery("aggregate");
            return pipeline;
        }

        @Override
        public Object buildCountQuery(Map<String, Object> criteria) {
            recordQuery("count");
            return criteria;
        }

        @Override
        public Object buildDistinctQuery(String fieldName, Map<String, Object> criteria) {
            recordQuery("distinct");
            return Map.of("field", fieldName, "query", criteria);
        }

        @Override
        public Object buildCreateIndexQuery(Map<String, Object> indexSpec, Map<String, Object> options) {
            recordQuery("createIndex");
            return Map.of("keys", indexSpec, "options", options);
        }

        @Override
        public Object buildTextSearchQuery(String searchText, Map<String, Object> parameters) {
            recordQuery("textSearch");
            return Map.of("$text", Map.of("$search", searchText));
        }

        @Override
        public Object buildGeospatialQuery(Map<String, Object> location, Map<String, Object> parameters) {
            recordQuery("geoSearch");
            return Map.of("location", Map.of("$near", location));
        }

        @Override
        public Object buildRegexQuery(String field, String pattern, Map<String, Object> options) {
            recordQuery("regex");
            var regexOptions = options.getOrDefault("options", "i");
            return Map.of(field, Map.of("$regex", pattern, "$options", regexOptions));
        }

        @Override
        public String getCollectionName() {
            return "generic_collection";
        }

        @Override
        public String getDatabaseType() {
            return "Generic Document Database";
        }

        @Override
        public Map<String, Object> getQueryStats() {
            return Map.copyOf(queryStats);
        }

        @Override
        public void resetQueryStats() {
            queryStats.clear();
        }

        private void recordQuery(String queryType) {
            queryStats.merge(queryType + "_count", 1L, (a, b) -> (Long)a + (Long)b);
        }
    }
}