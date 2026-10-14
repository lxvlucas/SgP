package com.SgP.demo.support;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;

/** Cada contexto/cenário recebe um schema próprio, dentro de uma base exclusiva. */
public final class PostgresTestDatabase implements AutoCloseable {
    private final String url;
    private final String username;
    private final String password;
    private final String schema = "sgp_test_" + UUID.randomUUID().toString().replace("-", "");

    public PostgresTestDatabase() throws SQLException {
        url = required("TEST_DATABASE_URL");
        username = required("TEST_DATABASE_USERNAME");
        password = required("TEST_DATABASE_PASSWORD");
        validateUrl(url);
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
    }

    public static void validateUrl(String url) {
        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("TEST_DATABASE_URL deve ser uma URL JDBC PostgreSQL explícita.");
        }
        var uri = URI.create(url.substring(5));
        // Bloqueia parâmetros que possam redirecionar a conexão ou o search_path.
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || uri.getPath() == null
                || !uri.getPath().matches("/sgp_test(?:_[a-z0-9_]+)?")) {
            throw new IllegalArgumentException("Use uma base exclusiva sgp_test ou sgp_test_*, sem parâmetros na URL.");
        }
    }

    public Connection connect() throws SQLException {
        var connection = DriverManager.getConnection(url, username, password);
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT current_database()")) {
            result.next();
            if (!result.getString(1).equals(URI.create(url.substring(5)).getPath().substring(1))) {
                throw new SQLException("A conexão não aponta para a base de teste solicitada.");
            }
            return connection;
        } catch (Exception exception) {
            connection.close();
            throw exception;
        }
    }

    public Connection connectToSchema() throws SQLException {
        var connection = connect();
        connection.setSchema(schema);
        return connection;
    }

    public Flyway flyway() {
        return Flyway.configure().dataSource(url, username, password)
                .schemas(schema).defaultSchema(schema).createSchemas(false)
                .baselineOnMigrate(false).baselineVersion("1").cleanDisabled(true)
                .load();
    }

    public String url() { return url; }
    public String username() { return username; }
    public String password() { return password; }
    public String schema() { return schema; }

    @Override
    public void close() throws SQLException {
        // O identificador é gerado internamente; nunca aceitamos nomes externos para limpeza.
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private static String required(String name) {
        var value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " ausente. Execute scripts/test-postgresql.ps1 ou configure a base exclusiva de testes.");
        }
        return value;
    }
}
