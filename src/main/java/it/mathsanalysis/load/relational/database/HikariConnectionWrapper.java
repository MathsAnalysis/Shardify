package it.mathsanalysis.load.relational.database;

import it.mathsanalysis.load.spi.database.Connection;
import it.mathsanalysis.load.spi.database.PreparedStatement;
import it.mathsanalysis.load.relational.connection.HikariConnectionProvider;

import java.sql.SQLException;

/**
 * Wrapper for JDBC connections from HikariCP
 * Provides the SPI abstraction layer while maintaining performance
 */
public final class HikariConnectionWrapper implements Connection {
    
    private final java.sql.Connection jdbcConnection;
    private final HikariConnectionProvider provider;
    private volatile boolean closed = false;
    
    public HikariConnectionWrapper(java.sql.Connection jdbcConnection, HikariConnectionProvider provider) {
        this.jdbcConnection = jdbcConnection;
        this.provider = provider;
    }
    
    @Override
    public PreparedStatement prepareStatement(String sql) {
        if (closed) {
            throw new IllegalStateException("Connection is closed");
        }
        
        try {
            var jdbcStatement = jdbcConnection.prepareStatement(sql);
            return new JdbcPreparedStatementWrapper(jdbcStatement);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to prepare statement: " + sql, e);
        }
    }
    
    @Override
    public PreparedStatement prepareStatement(String sql, boolean returnGeneratedKeys) {
        if (closed) {
            throw new IllegalStateException("Connection is closed");
        }
        
        try {
            var jdbcStatement = returnGeneratedKeys 
                ? jdbcConnection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
                : jdbcConnection.prepareStatement(sql);
                
            return new JdbcPreparedStatementWrapper(jdbcStatement);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to prepare statement with generated keys: " + sql, e);
        }
    }
    
    @Override
    public boolean isValid(int timeout) {
        if (closed) {
            return false;
        }
        
        try {
            return jdbcConnection.isValid(timeout);
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public void beginTransaction() {
        if (closed) {
            throw new IllegalStateException("Connection is closed");
        }
        
        try {
            jdbcConnection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to begin transaction", e);
        }
    }
    
    @Override
    public void commit() {
        if (closed) {
            throw new IllegalStateException("Connection is closed");
        }
        
        try {
            jdbcConnection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit transaction", e);
        }
    }
    
    @Override
    public void rollback() {
        if (closed) {
            throw new IllegalStateException("Connection is closed");
        }
        
        try {
            jdbcConnection.rollback();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rollback transaction", e);
        }
    }
    
    @Override
    public void setAutoCommit(boolean autoCommit) {
        if (closed) {
            throw new IllegalStateException("Connection is closed");
        }
        
        try {
            jdbcConnection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set auto-commit to " + autoCommit, e);
        }
    }
    
    @Override
    public boolean getAutoCommit() {
        if (closed) {
            throw new IllegalStateException("Connection is closed");
        }
        
        try {
            return jdbcConnection.getAutoCommit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get auto-commit status", e);
        }
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            provider.recordConnectionReturn();
            
            try {
                jdbcConnection.close();
            } catch (SQLException e) {
                // Log but don't throw - connection is being returned to pool
                System.err.println("Warning: Exception closing connection: " + e.getMessage());
            }
        }
    }
    
    // Package-private access to underlying JDBC connection if needed
    java.sql.Connection getJdbcConnection() {
        return jdbcConnection;
    }
}