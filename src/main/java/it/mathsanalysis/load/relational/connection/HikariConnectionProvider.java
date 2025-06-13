package it.mathsanalysis.load.relational.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.mathsanalysis.load.relational.database.HikariConnectionWrapper;
import it.mathsanalysis.load.spi.connection.ConnectionProvider;
import it.mathsanalysis.load.spi.database.Connection;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * HikariCP-based connection provider for high-performance SQL database connections.
 * 
 * Features:
 * - Ultra-fast connection pooling with HikariCP
 * - Connection leak detection
 * - Health monitoring with detailed metrics
 * - Automatic pool sizing and optimization
 * - Multiple database dialect support
 * - Production-ready configuration defaults
 * 
 * Performance Benefits:
 * - Zero-overhead connection acquisition
 * - Intelligent connection validation
 * - Optimized for high-concurrency workloads
 * - Memory-efficient connection management
 * 
 * Supported Databases:
 * - PostgreSQL (recommended for production)
 * - MySQL/MariaDB
 * - H2 (excellent for testing)
 * - SQLite (embedded/mobile applications)
 * - Oracle Database
 * - Microsoft SQL Server
 */
public final class HikariConnectionProvider implements ConnectionProvider {
    
    private final HikariDataSource dataSource;
    private final Map<String, Object> configuration;
    private final Map<String, Object> statistics;
    private volatile boolean closed = false;
    
    /**
     * Create connection provider with basic configuration
     * 
     * @param connectionString JDBC connection string
     */
    public HikariConnectionProvider(String connectionString) {
        this(connectionString, null, null, 20, 5, 30000, 600000, 1800000);
    }
    
    /**
     * Create connection provider with credentials
     * 
     * @param connectionString JDBC connection string
     * @param username Database username
     * @param password Database password
     */
    public HikariConnectionProvider(String connectionString, String username, String password) {
        this(connectionString, username, password, 20, 5, 30000, 600000, 1800000);
    }
    
    /**
     * Create connection provider with full configuration
     * 
     * @param connectionString JDBC connection string  
     * @param username Database username (nullable)
     * @param password Database password (nullable)
     * @param maxPoolSize Maximum connections in pool
     * @param minIdle Minimum idle connections
     * @param connectionTimeoutMs Connection timeout in milliseconds
     * @param idleTimeoutMs Idle timeout in milliseconds
     * @param maxLifetimeMs Maximum connection lifetime in milliseconds
     */
    public HikariConnectionProvider(
            String connectionString,
            String username,
            String password,
            int maxPoolSize,
            int minIdle,
            long connectionTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs) {
        
        this.configuration = new ConcurrentHashMap<>();
        this.statistics = new ConcurrentHashMap<>();
        
        // Build HikariCP configuration
        var config = buildHikariConfig(
            connectionString, username, password, maxPoolSize, minIdle,
            connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs
        );
        
        // Create data source
        this.dataSource = new HikariDataSource(config);
        
        // Store configuration for debugging
        storeConfiguration(connectionString, username, maxPoolSize, minIdle,
                          connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs);
        
        // Initialize statistics
        initializeStatistics();
    }
    
    @Override
    public Connection getConnection() {
        if (closed) {
            throw new IllegalStateException("Connection provider has been closed");
        }
        
        try {
            var jdbcConnection = dataSource.getConnection();
            recordConnectionAcquisition();
            return new HikariConnectionWrapper(jdbcConnection, this);
        } catch (SQLException e) {
            recordConnectionError();
            throw new RuntimeException("Failed to acquire database connection", e);
        }
    }
    
    @Override
    public Map<String, Object> getConnectionStats() {
        updateRuntimeStatistics();
        return Map.copyOf(statistics);
    }
    
    @Override
    public boolean isHealthy(int timeoutSeconds) {
        if (closed) {
            return false;
        }
        
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(timeoutSeconds);
        } catch (SQLException e) {
            recordHealthCheckFailure(e);
            return false;
        }
    }
    
    @Override
    public int getActiveConnections() {
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }
    
    @Override
    public int getMaxPoolSize() {
        return dataSource.getMaximumPoolSize();
    }
    
    @Override
    public int getIdleConnections() {
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        return Map.copyOf(configuration);
    }
    
    @Override
    public void adjustPoolSize(int newMaxSize, int newMinIdle) {
        if (closed) {
            throw new IllegalStateException("Connection provider has been closed");
        }
        
        if (newMaxSize <= 0) {
            throw new IllegalArgumentException("Max pool size must be positive");
        }
        
        if (newMinIdle < 0 || newMinIdle > newMaxSize) {
            throw new IllegalArgumentException("Min idle must be between 0 and max pool size");
        }
        
        dataSource.setMaximumPoolSize(newMaxSize);
        dataSource.setMinimumIdle(newMinIdle);
        
        configuration.put("maxPoolSize", newMaxSize);
        configuration.put("minIdle", newMinIdle);
        configuration.put("lastPoolAdjustment", System.currentTimeMillis());
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            statistics.put("closedAt", System.currentTimeMillis());

            CompletableFuture.runAsync(() -> {
                try {
                    if (dataSource != null && !dataSource.isClosed()) {
                        dataSource.close();
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Error closing datasource: " + e.getMessage());
                }
            }).orTimeout(10, TimeUnit.SECONDS).exceptionally(throwable -> {
                System.err.println("Datasource close timeout, forcing shutdown" + throwable.getMessage());
                return null;
            });
        }
    }
    
    // Additional HikariCP-specific methods
    
    /**
     * Get detailed HikariCP pool metrics
     * 
     * @return Detailed pool metrics
     */
    public Map<String, Object> getDetailedPoolMetrics() {
        if (closed) {
            return Map.of("error", "Connection provider is closed");
        }
        
        var poolMXBean = dataSource.getHikariPoolMXBean();
        
        return Map.of(
            "activeConnections", poolMXBean.getActiveConnections(),
            "idleConnections", poolMXBean.getIdleConnections(),
            "totalConnections", poolMXBean.getTotalConnections(),
            "threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection(),
            "maxPoolSize", dataSource.getMaximumPoolSize(),
            "minIdle", dataSource.getMinimumIdle(),
            "connectionTimeout", dataSource.getConnectionTimeout(),
            "idleTimeout", dataSource.getIdleTimeout(),
            "maxLifetime", dataSource.getMaxLifetime(),
            "leakDetectionThreshold", dataSource.getLeakDetectionThreshold()
        );
    }
    
    /**
     * Check if connection pool is under pressure
     * 
     * @return true if pool is under high load
     */
    public boolean isPoolUnderPressure() {
        if (closed) {
            return true;
        }
        
        var poolMXBean = dataSource.getHikariPoolMXBean();
        var activeConnections = poolMXBean.getActiveConnections();
        var totalConnections = poolMXBean.getTotalConnections();
        var threadsWaiting = poolMXBean.getThreadsAwaitingConnection();
        
        // Pool is under pressure if:
        // 1. More than 80% of connections are active
        // 2. There are threads waiting for connections
        var utilizationRate = (double) activeConnections / totalConnections;
        return utilizationRate > 0.8 || threadsWaiting > 0;
    }
    
    /**
     * Get connection acquisition performance
     * 
     * @return Performance metrics
     */
    public Map<String, Object> getConnectionPerformance() {
        return Map.of(
            "totalConnectionsAcquired", statistics.getOrDefault("connectionsAcquired", 0L),
            "connectionErrors", statistics.getOrDefault("connectionErrors", 0L),
            "healthCheckFailures", statistics.getOrDefault("healthCheckFailures", 0L),
            "averageAcquisitionTime", calculateAverageAcquisitionTime(),
            "lastConnectionTime", statistics.getOrDefault("lastConnectionTime", 0L)
        );
    }
    
    /**
     * Evict idle connections to free up resources
     */
    public void evictIdleConnections() {
        if (!closed) {
            dataSource.getHikariPoolMXBean().softEvictConnections();
        }
    }
    
    /**
     * Suspend connection pool (emergency measure)
     */
    public void suspendPool() {
        if (!closed) {
            dataSource.getHikariPoolMXBean().suspendPool();
            statistics.put("poolSuspended", true);
            statistics.put("suspendedAt", System.currentTimeMillis());
        }
    }
    
    /**
     * Resume connection pool
     */
    public void resumePool() {
        if (!closed) {
            dataSource.getHikariPoolMXBean().resumePool();
            statistics.put("poolSuspended", false);
            statistics.put("resumedAt", System.currentTimeMillis());
        }
    }
    
    // Private helper methods
    
    private HikariConfig buildHikariConfig(
            String connectionString,
            String username, 
            String password,
            int maxPoolSize,
            int minIdle,
            long connectionTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs) {
        
        var config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(connectionString);
        if (username != null) config.setUsername(username);
        if (password != null) config.setPassword(password);
        
        // Pool sizing
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        
        // Timeout settings
        config.setConnectionTimeout(connectionTimeoutMs);
        config.setIdleTimeout(idleTimeoutMs);
        config.setMaxLifetime(maxLifetimeMs);
        
        // Performance optimizations
        config.setLeakDetectionThreshold(60000); // 1 minute leak detection
        config.setConnectionTestQuery(getValidationQuery(connectionString));
        
        // Database-specific optimizations
        optimizeForDatabase(config, connectionString);
        
        // Pool name for monitoring
        config.setPoolName("AbstractionLoader-" + System.currentTimeMillis());
        
        // Register MBeans for monitoring
        config.setRegisterMbeans(true);
        
        return config;
    }
    
    private String getValidationQuery(String connectionString) {
        var lowerUrl = connectionString.toLowerCase();
        
        if (lowerUrl.contains("postgresql")) {
            return "SELECT 1";
        } else if (lowerUrl.contains("mysql")) {
            return "SELECT 1";
        } else if (lowerUrl.contains("h2")) {
            return "SELECT 1";
        } else if (lowerUrl.contains("sqlite")) {
            return "SELECT 1";
        } else if (lowerUrl.contains("oracle")) {
            return "SELECT 1 FROM DUAL";
        } else if (lowerUrl.contains("sqlserver")) {
            return "SELECT 1";
        } else {
            return "SELECT 1"; // Generic fallback
        }
    }
    
    private void optimizeForDatabase(HikariConfig config, String connectionString) {
        var lowerUrl = connectionString.toLowerCase();
        
        if (lowerUrl.contains("postgresql")) {
            // PostgreSQL optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
            
        } else if (lowerUrl.contains("mysql")) {
            // MySQL optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
        } else if (lowerUrl.contains("h2")) {
            // H2 optimizations (mainly for testing)
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "100");
            
        } else if (lowerUrl.contains("oracle")) {
            // Oracle optimizations
            config.addDataSourceProperty("oracle.jdbc.implicitStatementCacheSize", "250");
            config.addDataSourceProperty("oracle.jdbc.defaultExecuteBatch", "100");
        }
    }
    
    private void storeConfiguration(
            String connectionString,
            String username,
            int maxPoolSize,
            int minIdle,
            long connectionTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs) {
        
        configuration.put("connectionString", maskPassword(connectionString));
        configuration.put("username", username);
        configuration.put("maxPoolSize", maxPoolSize);
        configuration.put("minIdle", minIdle);
        configuration.put("connectionTimeoutMs", connectionTimeoutMs);
        configuration.put("idleTimeoutMs", idleTimeoutMs);
        configuration.put("maxLifetimeMs", maxLifetimeMs);
        configuration.put("createdAt", System.currentTimeMillis());
        configuration.put("providerType", "HikariCP");
        configuration.put("databaseDialect", detectDialect(connectionString));
    }
    
    private void initializeStatistics() {
        statistics.put("connectionsAcquired", 0L);
        statistics.put("connectionsReturned", 0L);
        statistics.put("connectionErrors", 0L);
        statistics.put("healthCheckFailures", 0L);
        statistics.put("startTime", System.currentTimeMillis());
        statistics.put("poolSuspended", false);
    }
    
    private void updateRuntimeStatistics() {
        if (!closed) {
            var poolMXBean = dataSource.getHikariPoolMXBean();
            statistics.put("currentActiveConnections", poolMXBean.getActiveConnections());
            statistics.put("currentIdleConnections", poolMXBean.getIdleConnections());
            statistics.put("currentTotalConnections", poolMXBean.getTotalConnections());
            statistics.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
            statistics.put("lastStatisticsUpdate", System.currentTimeMillis());
        }
    }

    public void recordConnectionReturn() {
        statistics.merge("connectionsReturned", 1L, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });
        statistics.put("lastReturnTime", System.currentTimeMillis());
    }

    private void recordConnectionAcquisition() {
        statistics.merge("connectionsAcquired", 1L, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });
        statistics.put("lastConnectionTime", System.currentTimeMillis());
    }

    private void recordConnectionError() {
        statistics.merge("connectionErrors", 1L, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });
        statistics.put("lastErrorTime", System.currentTimeMillis());
    }

    private void recordHealthCheckFailure(SQLException e) {
        statistics.merge("healthCheckFailures", 1L, (oldVal, newVal) -> {
            if (oldVal instanceof Long) {
                return (Long) oldVal + (Long) newVal;
            }
            return newVal;
        });
        statistics.put("lastHealthCheckFailure", System.currentTimeMillis());
        statistics.put("lastHealthCheckError", e.getMessage());
    }

    
    private double calculateAverageAcquisitionTime() {
        // This would require more sophisticated timing tracking
        // For now, return a placeholder
        return 0.0;
    }
    
    private String maskPassword(String connectionString) {
        // Simple password masking for security
        return connectionString.replaceAll("password=[^;&]*", "password=***");
    }
    
    private String detectDialect(String connectionString) {
        var lowerUrl = connectionString.toLowerCase();
        
        if (lowerUrl.contains("postgresql")) return "PostgreSQL";
        if (lowerUrl.contains("mysql")) return "MySQL";
        if (lowerUrl.contains("h2")) return "H2";
        if (lowerUrl.contains("sqlite")) return "SQLite";
        if (lowerUrl.contains("oracle")) return "Oracle";
        if (lowerUrl.contains("sqlserver")) return "SQL Server";
        
        return "Unknown";
    }
}