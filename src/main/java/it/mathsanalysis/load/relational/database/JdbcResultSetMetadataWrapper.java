package it.mathsanalysis.load.relational.database;

import it.mathsanalysis.load.spi.database.ResultSetMetadata;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for JDBC ResultSetMetaData
 */
public final class JdbcResultSetMetadataWrapper implements ResultSetMetadata {
    
    private final java.sql.ResultSetMetaData jdbcMetadata;
    
    public JdbcResultSetMetadataWrapper(java.sql.ResultSetMetaData jdbcMetadata) {
        this.jdbcMetadata = jdbcMetadata;
    }
    
    @Override
    public int getColumnCount() {
        try {
            return jdbcMetadata.getColumnCount();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get column count", e);
        }
    }
    
    @Override
    public String getColumnName(int columnIndex) {
        try {
            return jdbcMetadata.getColumnName(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get column name for index: " + columnIndex, e);
        }
    }
    
    @Override
    public String getColumnType(int columnIndex) {
        try {
            return jdbcMetadata.getColumnTypeName(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get column type for index: " + columnIndex, e);
        }
    }
    
    @Override
    public List<String> getColumnNames() {
        try {
            var names = new ArrayList<String>();
            var columnCount = jdbcMetadata.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                names.add(jdbcMetadata.getColumnName(i));
            }
            
            return names;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get all column names", e);
        }
    }
    
    @Override
    public boolean isNullable(int columnIndex) {
        try {
            return jdbcMetadata.isNullable(columnIndex) != java.sql.ResultSetMetaData.columnNoNulls;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if column is nullable: " + columnIndex, e);
        }
    }
    
    @Override
    public int getPrecision(int columnIndex) {
        try {
            return jdbcMetadata.getPrecision(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get precision for column: " + columnIndex, e);
        }
    }
    
    @Override
    public int getScale(int columnIndex) {
        try {
            return jdbcMetadata.getScale(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get scale for column: " + columnIndex, e);
        }
    }
}