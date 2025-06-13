package it.mathsanalysis.load.builder;

import it.mathsanalysis.load.core.DataLoader;
import it.mathsanalysis.load.impl.mapping.ReflectionItemMapper;
import it.mathsanalysis.load.impl.serialization.JacksonDocumentSerializer;
import it.mathsanalysis.load.relational.impl.SqlDataLoader;
import it.mathsanalysis.load.document.impl.MongoDataLoader;
import it.mathsanalysis.load.relational.connection.HikariConnectionProvider;
import it.mathsanalysis.load.document.connection.MongoConnectionProvider;
import it.mathsanalysis.load.impl.query.ReflectionQueryBuilder;
import it.mathsanalysis.load.document.query.MongoQueryBuilder;
import it.mathsanalysis.load.spi.connection.ConnectionProvider;
import it.mathsanalysis.load.spi.connection.DocumentConnectionProvider;
import it.mathsanalysis.load.spi.query.DocumentQueryBuilder;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Fluent builder for creating optimized data loaders.
 */
public final class LoaderBuilder<T, ID> {

    // Core configuration
    private final Class<T> itemType;
    private final Class<ID> idType;
    private final Map<String, Object> configuration;

    // Database type
    private LoaderType loaderType;

    // Connection configuration
    private ConnectionProvider connectionProvider;
    private DocumentConnectionProvider documentConnectionProvider;
    private String connectionString;
    private String username;
    private String password;
    private int maxPoolSize = 20;
    private int minIdle = 5;
    private long connectionTimeoutMs = 30000;
    private long idleTimeoutMs = 600000;
    private long maxLifetimeMs = 1800000;

    // Database configuration
    private String tableName;
    private String collectionName;
    private String databaseName;
    private String schema;

    // Feature flags
    private boolean enableMetrics = true;
    private boolean enableQueryCache = true;
    private final boolean enableConnectionPooling = true;
    private boolean enableHealthChecks = true;
    private boolean enableAsyncOperations = true;

    // Performance tuning
    private int batchSize = 1000;
    private long queryTimeoutMs = 30000;
    private int maxCacheSize = 1000;
    private final boolean enablePreparedStatementCache = true;

    // Environment-specific settings
    private Environment environment = Environment.STANDALONE;
    private String minecraftPluginName;

    /**
     * Private constructor - use factory methods
     */
    private LoaderBuilder(Class<T> itemType, Class<ID> idType) {
        this.itemType = Objects.requireNonNull(itemType, "Item type cannot be null");
        this.idType = Objects.requireNonNull(idType, "ID type cannot be null");
        this.configuration = new java.util.HashMap<>();
        initializeDefaults();
    }

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
        return new LoaderBuilder<>(itemType, String.class);
    }

    public LoaderBuilder<T, ID> forMinecraftOptimization(String pluginName) {
        forMinecraft(pluginName);
        this.maxPoolSize = 1;
        this.minIdle = 0;
        this.batchSize = 20;
        this.queryTimeoutMs = 1000;
        this.maxCacheSize = 100;
        this.enableQueryCache = false;
        return this;
    }


    // SQL Database Configuration

    /**
     * Configure for SQL database with connection string
     */
    public LoaderBuilder<T, ID> withSqlConnection(String connectionString) {
        this.connectionString = Objects.requireNonNull(connectionString, "Connection string cannot be null");
        this.loaderType = LoaderType.SQL;
        detectSqlDialect(connectionString);
        return this;
    }

    /**
     * Configure for SQL database with credentials
     */
    public LoaderBuilder<T, ID> withSqlConnection(String connectionString, String username, String password) {
        this.connectionString = Objects.requireNonNull(connectionString, "Connection string cannot be null");
        this.username = username;
        this.password = password;
        this.loaderType = LoaderType.SQL;
        detectSqlDialect(connectionString);
        return this;
    }

    /**
     * Configure with custom SQL connection provider
     */
    public LoaderBuilder<T, ID> withSqlConnection(ConnectionProvider provider) {
        this.connectionProvider = Objects.requireNonNull(provider, "Connection provider cannot be null");
        this.loaderType = LoaderType.SQL;
        return this;
    }

    /**
     * Set table name for SQL databases
     */
    public LoaderBuilder<T, ID> withTable(String tableName) {
        this.tableName = Objects.requireNonNull(tableName, "Table name cannot be null");
        return this;
    }

    /**
     * Set database schema for SQL databases
     */
    public LoaderBuilder<T, ID> withSchema(String schema) {
        this.schema = schema;
        return this;
    }

    // Document Database Configuration

    /**
     * Configure for MongoDB connection
     */
    public LoaderBuilder<T, ID> withMongoConnection(String connectionString, String databaseName) {
        this.connectionString = Objects.requireNonNull(connectionString, "Connection string cannot be null");
        this.databaseName = Objects.requireNonNull(databaseName, "Database name cannot be null");
        this.loaderType = LoaderType.MONGO;
        return this;
    }

    /**
     * Configure with custom document connection provider
     */
    public LoaderBuilder<T, ID> withDocumentConnection(DocumentConnectionProvider provider) {
        this.documentConnectionProvider = Objects.requireNonNull(provider, "Document connection provider cannot be null");
        this.loaderType = LoaderType.DOCUMENT;
        return this;
    }

    /**
     * Set collection name for document databases
     */
    public LoaderBuilder<T, ID> withCollection(String collectionName) {
        this.collectionName = Objects.requireNonNull(collectionName, "Collection name cannot be null");
        return this;
    }

    /**
     * Set database name for document databases
     */
    public LoaderBuilder<T, ID> withDatabase(String databaseName) {
        this.databaseName = Objects.requireNonNull(databaseName, "Database name cannot be null");
        return this;
    }

    // Connection Pool Configuration

    /**
     * Configure connection pool settings
     */
    public LoaderBuilder<T, ID> withConnectionPool(int maxPoolSize, int minIdle) {
        if (maxPoolSize <= 0) throw new IllegalArgumentException("Max pool size must be positive");
        if (minIdle < 0) throw new IllegalArgumentException("Min idle cannot be negative");
        if (minIdle > maxPoolSize) throw new IllegalArgumentException("Min idle cannot exceed max pool size");

        this.maxPoolSize = maxPoolSize;
        this.minIdle = minIdle;
        return this;
    }

    /**
     * Configure advanced connection pool settings
     */
    public LoaderBuilder<T, ID> withAdvancedConnectionPool(
            int maxPoolSize,
            int minIdle,
            long connectionTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs) {

        withConnectionPool(maxPoolSize, minIdle);
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.idleTimeoutMs = idleTimeoutMs;
        this.maxLifetimeMs = maxLifetimeMs;
        return this;
    }

    // Feature Configuration

    /**
     * Enable or disable performance metrics collection
     */
    public LoaderBuilder<T, ID> withMetrics(boolean enabled) {
        this.enableMetrics = enabled;
        return this;
    }

    /**
     * Enable or disable query caching
     */
    public LoaderBuilder<T, ID> withQueryCache(boolean enabled) {
        this.enableQueryCache = enabled;
        return this;
    }

    /**
     * Configure batch processing settings
     */
    public LoaderBuilder<T, ID> withBatchSize(int batchSize) {
        if (batchSize <= 0) throw new IllegalArgumentException("Batch size must be positive");
        this.batchSize = batchSize;
        return this;
    }

    /**
     * Configure query timeout
     */
    public LoaderBuilder<T, ID> withQueryTimeout(long timeoutMs) {
        if (timeoutMs <= 0) throw new IllegalArgumentException("Query timeout must be positive");
        this.queryTimeoutMs = timeoutMs;
        return this;
    }

    // Environment Configuration

    /**
     * Configure for Minecraft environment
     */
    public LoaderBuilder<T, ID> forMinecraft(String pluginName) {
        this.environment = Environment.MINECRAFT_SPIGOT;
        this.minecraftPluginName = Objects.requireNonNull(pluginName, "Plugin name cannot be null");

        // Minecraft-specific optimizations
        this.maxPoolSize = Math.min(maxPoolSize, 10);
        this.enableAsyncOperations = true;
        this.queryTimeoutMs = Math.min(queryTimeoutMs, 5000);

        return this;
    }

    /**
     * Configure for Spring Boot environment
     */
    public LoaderBuilder<T, ID> forSpringBoot() {
        this.environment = Environment.SPRING_BOOT;
        this.enableHealthChecks = true;
        this.enableMetrics = true;
        return this;
    }

    /**
     * Configure for standalone application
     */
    public LoaderBuilder<T, ID> forStandalone() {
        this.environment = Environment.STANDALONE;
        return this;
    }

    // Build Methods

    /**
     * Build the configured data loader
     */
    public DataLoader<T, ID> build() {
        validateConfiguration();
        applyDefaults();
        buildConfiguration();

        return switch (loaderType) {
            case SQL -> buildSqlLoader();
            case MONGO -> buildMongoLoader();
            case DOCUMENT -> buildDocumentLoader();
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

    // Private Implementation Methods

    private void initializeDefaults() {
        configuration.put("enableMetrics", enableMetrics);
        configuration.put("enableQueryCache", enableQueryCache);
        configuration.put("batchSize", batchSize);
        configuration.put("queryTimeoutMs", queryTimeoutMs);
        configuration.put("environment", environment);
    }

    private void validateConfiguration() {
        if (loaderType == null) {
            throw new IllegalStateException("Database type not specified. Use withSqlConnection() or withMongoConnection()");
        }

        if (loaderType == LoaderType.SQL) {
            if (connectionProvider == null && connectionString == null) {
                throw new IllegalStateException("SQL connection not configured");
            }
        }

        if (loaderType == LoaderType.MONGO || loaderType == LoaderType.DOCUMENT) {
            if (documentConnectionProvider == null && connectionString == null) {
                throw new IllegalStateException("Document connection not configured");
            }
            if (loaderType == LoaderType.MONGO && databaseName == null) {
                throw new IllegalStateException("Database name required for MongoDB");
            }
        }
    }

    private void applyDefaults() {
        if (tableName == null && loaderType == LoaderType.SQL) {
            tableName = generateDefaultTableName();
        }

        if (collectionName == null && (loaderType == LoaderType.MONGO || loaderType == LoaderType.DOCUMENT)) {
            collectionName = generateDefaultCollectionName();
        }
    }

    private void buildConfiguration() {
        configuration.put("enableMetrics", enableMetrics);
        configuration.put("enableQueryCache", enableQueryCache);
        configuration.put("batchSize", batchSize);
        configuration.put("queryTimeoutMs", queryTimeoutMs);
        configuration.put("environment", environment);
        configuration.put("maxPoolSize", maxPoolSize);
        configuration.put("minIdle", minIdle);
        configuration.put("connectionTimeoutMs", connectionTimeoutMs);
        configuration.put("idleTimeoutMs", idleTimeoutMs);
        configuration.put("maxLifetimeMs", maxLifetimeMs);
    }

    private DataLoader<T, ID> buildSqlLoader() {
        // Create connection provider if not provided
        if (connectionProvider == null) {
            connectionProvider = createSqlConnectionProvider();
        }

        // Create SQL-specific components
        var queryBuilder = new ReflectionQueryBuilder<T>(itemType, tableName, enableQueryCache);
        var itemMapper = new ReflectionItemMapper<>(itemType);

        return new SqlDataLoader<>(
                itemType,
                idType,
                connectionProvider,
                queryBuilder,
                itemMapper,
                tableName,
                configuration
        );
    }

    private DataLoader<T, ID> buildMongoLoader() {
        // Create connection provider if not provided
        if (documentConnectionProvider == null) {
            documentConnectionProvider = new MongoConnectionProvider(connectionString, databaseName);
        }

        // Create MongoDB-specific components
        var serializer = new JacksonDocumentSerializer<T>();
        var queryBuilder = new MongoQueryBuilder<T>();

        return new MongoDataLoader<>(
                itemType,
                idType,
                documentConnectionProvider,
                serializer,
                queryBuilder,
                collectionName,
                databaseName,
                configuration
        );
    }

    private DataLoader<T, ID> buildDocumentLoader() {
        // Generic document loader implementation
        var serializer = new JacksonDocumentSerializer<T>();
        var queryBuilder = new GenericDocumentQueryBuilder<T>();

        return new MongoDataLoader<>(
                itemType,
                idType,
                documentConnectionProvider,
                serializer,
                queryBuilder,
                collectionName,
                databaseName != null ? databaseName : "default",
                configuration
        );
    }

    private ConnectionProvider createSqlConnectionProvider() {
        return new HikariConnectionProvider(
                connectionString,
                username,
                password,
                maxPoolSize,
                minIdle,
                connectionTimeoutMs,
                idleTimeoutMs,
                maxLifetimeMs
        );
    }

    private void detectSqlDialect(String connectionString) {
        var dialect = "generic";
        if (connectionString.contains("postgresql")) {
            dialect = "postgresql";
        } else if (connectionString.contains("mysql")) {
            dialect = "mysql";
        } else if (connectionString.contains("h2")) {
            dialect = "h2";
        } else if (connectionString.contains("sqlite")) {
            dialect = "sqlite";
        } else if (connectionString.contains("oracle")) {
            dialect = "oracle";
        } else if (connectionString.contains("sqlserver")) {
            dialect = "sqlserver";
        }
        configuration.put("sqlDialect", dialect);
    }

    private String generateDefaultTableName() {
        return itemType.getSimpleName().toLowerCase() + "s";
    }

    private String generateDefaultCollectionName() {
        return itemType.getSimpleName().toLowerCase() + "s";
    }

    // Enums and Helper Classes

    private enum LoaderType {
        SQL, MONGO, DOCUMENT
    }

    public enum Environment {
        MINECRAFT_SPIGOT,
        MINECRAFT_FABRIC,
        SPRING_BOOT,
        STANDALONE
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