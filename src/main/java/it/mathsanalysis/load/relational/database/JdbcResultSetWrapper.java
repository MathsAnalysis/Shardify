package it.mathsanalysis.load.relational.database;

import it.mathsanalysis.load.spi.database.ResultSet;
import it.mathsanalysis.load.spi.database.ResultSetMetadata;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Wrapper for JDBC ResultSet
 * Provides SPI abstraction with type-safe value extraction
 */
public final class JdbcResultSetWrapper implements ResultSet {
    
    private final java.sql.ResultSet jdbcResultSet;
    private volatile boolean closed = false;
    
    public JdbcResultSetWrapper(java.sql.ResultSet jdbcResultSet) {
        this.jdbcResultSet = jdbcResultSet;
    }
    
    @Override
    public boolean next() {
        if (closed) {
            throw new IllegalStateException("ResultSet is closed");
        }
        
        try {
            return jdbcResultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to move to next row", e);
        }
    }
    
    @Override
    public Object getValue(String columnName) {
        if (closed) {
            throw new IllegalStateException("ResultSet is closed");
        }
        
        try {
            return jdbcResultSet.getObject(columnName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get value for column: " + columnName, e);
        }
    }
    
    @Override
    public Object getValue(int columnIndex) {
        if (closed) {
            throw new IllegalStateException("ResultSet is closed");
        }
        
        try {
            return jdbcResultSet.getObject(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get value for column index: " + columnIndex, e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(String columnName, Class<T> type) {
        var value = getValue(columnName);
        if (value == null) {
            return null;
        }
        
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        
        // Type conversion
        return convertValue(value, type);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(int columnIndex, Class<T> type) {
        var value = getValue(columnIndex);
        if (value == null) {
            return null;
        }
        
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        
        // Type conversion
        return convertValue(value, type);
    }
    
    @Override
    public String getString(String columnName) {
        try {
            return jdbcResultSet.getString(columnName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get string for column: " + columnName, e);
        }
    }
    
    @Override
    public Integer getInt(String columnName) {
        try {
            var value = jdbcResultSet.getInt(columnName);
            return jdbcResultSet.wasNull() ? null : value;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get int for column: " + columnName, e);
        }
    }
    
    @Override
    public Long getLong(String columnName) {
        try {
            var value = jdbcResultSet.getLong(columnName);
            return jdbcResultSet.wasNull() ? null : value;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get long for column: " + columnName, e);
        }
    }
    
    @Override
    public Double getDouble(String columnName) {
        try {
            var value = jdbcResultSet.getDouble(columnName);
            return jdbcResultSet.wasNull() ? null : value;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get double for column: " + columnName, e);
        }
    }
    
    @Override
    public Boolean getBoolean(String columnName) {
        try {
            var value = jdbcResultSet.getBoolean(columnName);
            return jdbcResultSet.wasNull() ? null : value;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get boolean for column: " + columnName, e);
        }
    }
    
    @Override
    public BigDecimal getBigDecimal(String columnName) {
        try {
            return jdbcResultSet.getBigDecimal(columnName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get BigDecimal for column: " + columnName, e);
        }
    }
    
    @Override
    public LocalDate getLocalDate(String columnName) {
        try {
            var date = jdbcResultSet.getDate(columnName);
            return date != null ? date.toLocalDate() : null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get LocalDate for column: " + columnName, e);
        }
    }
    
    @Override
    public LocalDateTime getLocalDateTime(String columnName) {
        try {
            var timestamp = jdbcResultSet.getTimestamp(columnName);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get LocalDateTime for column: " + columnName, e);
        }
    }
    
    @Override
    public boolean wasNull() {
        try {
            return jdbcResultSet.wasNull();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check null status", e);
        }
    }
    
    @Override
    public ResultSetMetadata getMetadata() {
        try {
            return new JdbcResultSetMetadataWrapper(jdbcResultSet.getMetaData());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get result set metadata", e);
        }
    }
    
    @Override
    public boolean isEmpty() {
        // This is expensive - consider caching if needed frequently
        try {
            return !jdbcResultSet.isBeforeFirst() && !jdbcResultSet.first();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if result set is empty", e);
        }
    }
    
    @Override
    public int getRow() {
        try {
            return jdbcResultSet.getRow();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get current row number", e);
        }
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                jdbcResultSet.close();
            } catch (SQLException e) {
                // Log but don't throw
                System.err.println("Warning: Exception closing ResultSet: " + e.getMessage());
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        
        var valueStr = value.toString();
        
        try {
            if (type == String.class) {
                return (T) valueStr;
            } else if (type == Integer.class || type == int.class) {
                return (T) Integer.valueOf(valueStr);
            } else if (type == Long.class || type == long.class) {
                return (T) Long.valueOf(valueStr);
            } else if (type == Double.class || type == double.class) {
                return (T) Double.valueOf(valueStr);
            } else if (type == Float.class || type == float.class) {
                return (T) Float.valueOf(valueStr);
            } else if (type == Boolean.class || type == boolean.class) {
                return (T) Boolean.valueOf(valueStr);
            } else if (type == BigDecimal.class) {
                return (T) new BigDecimal(valueStr);
            } else {
                return (T) value; // Hope for the best
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert value '" + value + "' to type " + type.getSimpleName(), e);
        }
    }
}