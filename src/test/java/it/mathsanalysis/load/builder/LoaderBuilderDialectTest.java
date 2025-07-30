package it.mathsanalysis.load.builder;

import it.mathsanalysis.load.core.DataLoader;
import it.mathsanalysis.load.spi.connection.ConnectionProvider;
import it.mathsanalysis.load.spi.database.Connection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for SQL dialect auto-detection in {@link LoaderBuilder}.
 */
class LoaderBuilderDialectTest {

    private static class DummyConnectionProvider implements ConnectionProvider {
        @Override
        public Connection getConnection() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Map<String, Object> getConnectionStats() { return java.util.Map.of(); }

        @Override
        public boolean isHealthy(int timeoutSeconds) { return true; }

        @Override
        public int getActiveConnections() { return 0; }

        @Override
        public int getMaxPoolSize() { return 0; }

        @Override
        public int getIdleConnections() { return 0; }

        @Override
        public java.util.Map<String, Object> getConfiguration() { return java.util.Map.of(); }

        @Override
        public void adjustPoolSize(int newMaxSize, int newMinIdle) {}

        @Override
        public void close() {}
    }

    @Test
    void detectPostgresDialect() {
        LoaderBuilder<Object, Long> builder = LoaderBuilder.forType(Object.class, Long.class);
        builder.withSqlConnection("jdbc:postgresql://localhost/db");
        builder.withSqlConnection(new DummyConnectionProvider());
        DataLoader<?, ?> loader = builder.build();
        assertEquals("postgresql", loader.getConfiguration().get("dialect"));
    }

    @Test
    void detectMysqlDialect() {
        LoaderBuilder<Object, Long> builder = LoaderBuilder.forType(Object.class, Long.class);
        builder.withSqlConnection("jdbc:mysql://localhost/db");
        builder.withSqlConnection(new DummyConnectionProvider());
        DataLoader<?, ?> loader = builder.build();
        assertEquals("mysql", loader.getConfiguration().get("dialect"));
    }

    @Test
    void detectGenericDialect() {
        LoaderBuilder<Object, Long> builder = LoaderBuilder.forType(Object.class, Long.class);
        builder.withSqlConnection("jdbc:unknown://localhost/db");
        builder.withSqlConnection(new DummyConnectionProvider());
        DataLoader<?, ?> loader = builder.build();
        assertEquals("generic", loader.getConfiguration().get("dialect"));
    }
}
