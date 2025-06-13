package it.mathsanalysis.load.relational.database;

import it.mathsanalysis.load.spi.database.PreparedStatement;
import it.mathsanalysis.load.spi.database.ResultSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for JDBC PreparedStatement
 * Provides SPI abstraction with parameter index management
 */
public final class JdbcPreparedStatementWrapper implements PreparedStatement {
    
    private final java.sql.PreparedStatement jdbcStatement;
    private final List<Object> parameters = new ArrayList<>();
    private int currentParameterIndex = 1;
    private volatile boolean closed = false;
    
    public JdbcPreparedStatementWrapper(java.sql.PreparedStatement jdbcStatement) {
        this.jdbcStatement = jdbcStatement;
    }
    
    @Override
    public void setParameter(int index, Object value) {
        if (closed) {
            throw new IllegalStateException("PreparedStatement is closed");
        }
        
        try {
            // Ensure parameters list is large enough
            while (parameters.size() < index) {
                parameters.add(null);
            }
            parameters.set(index - 1, value);
            
            // Set parameter on JDBC statement
            setJdbcParameter(index, value);
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set parameter at index " + index, e);
        }
    }
    
    @Override
    public void setParameter(Object value) {
        setParameter(currentParameterIndex++, value);
    }
    
    @Override
    public int executeUpdate() {
        if (closed) {
            throw new IllegalStateException("PreparedStatement is closed");
        }
        
        try {
            return jdbcStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute update", e);
        }
    }
    
    @Override
    public ResultSet executeQuery() {
        if (closed) {
            throw new IllegalStateException("PreparedStatement is closed");
        }
        
        try {
            var jdbcResultSet = jdbcStatement.executeQuery();
            return new JdbcResultSetWrapper(jdbcResultSet);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query", e);
        }
    }
    
    @Override
    public ResultSet getGeneratedKeys() {
        if (closed) {
            throw new IllegalStateException("PreparedStatement is closed");
        }
        
        try {
            var jdbcResultSet = jdbcStatement.getGeneratedKeys();
            return new JdbcResultSetWrapper(jdbcResultSet);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get generated keys", e);
        }
    }
    
    @Override
    public void addBatch() {
        if (closed) {
            throw new IllegalStateException("PreparedStatement is closed");
        }
        
        try {
            jdbcStatement.addBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add batch", e);
        }
    }
    
    @Override
    public int[] executeBatch() {
        if (closed) {
            throw new IllegalStateException("PreparedStatement is closed");
        }
        
        try {
            return jdbcStatement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute batch", e);
        }
    }
    
    @Override
    public void clearBatch() {
        if (closed) {
            throw new IllegalStateException("PreparedStatement is closed");
        }
        
        try {
            jdbcStatement.clearBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear batch", e);
        }
    }
    
    @Override
    public void resetParameterIndex() {
        currentParameterIndex = 1;
    }
    
    @Override
    public int getParameterCount() {
        return parameters.size();
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                jdbcStatement.close();
            } catch (SQLException e) {
                // Log but don't throw
                System.err.println("Warning: Exception closing PreparedStatement: " + e.getMessage());
            }
        }
    }
    
    private void setJdbcParameter(int index, Object value) throws SQLException {
        if (value == null) {
            jdbcStatement.setNull(index, java.sql.Types.NULL);
        } else if (value instanceof String) {
            jdbcStatement.setString(index, (String) value);
        } else if (value instanceof Integer) {
            jdbcStatement.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            jdbcStatement.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            jdbcStatement.setDouble(index, (Double) value);
        } else if (value instanceof Float) {
            jdbcStatement.setFloat(index, (Float) value);
        } else if (value instanceof Boolean) {
            jdbcStatement.setBoolean(index, (Boolean) value);
        } else if (value instanceof java.math.BigDecimal) {
            jdbcStatement.setBigDecimal(index, (java.math.BigDecimal) value);
        } else if (value instanceof java.time.LocalDate) {
            jdbcStatement.setDate(index, java.sql.Date.valueOf((java.time.LocalDate) value));
        } else if (value instanceof java.time.LocalDateTime) {
            jdbcStatement.setTimestamp(index, java.sql.Timestamp.valueOf((java.time.LocalDateTime) value));
        } else if (value instanceof byte[]) {
            jdbcStatement.setBytes(index, (byte[]) value);
        } else {
            // Fallback for other types
            jdbcStatement.setObject(index, value);
        }
    }
}