package com.SgP.demo;

import com.SgP.demo.support.PostgresTestDatabase;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresMigrationTests {
    @Test
    void migraBancoVazioSemContasDeDemoEReexecutaSemAlteracoes() throws Exception {
        try (var database = new PostgresTestDatabase()) {
            var flyway = database.flyway();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
            assertThat(flyway.migrate().migrationsExecuted).isZero();
            flyway.validate();
            try (var connection = database.connectToSchema(); var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("SELECT count(*) FROM usuario")) {
                    result.next();
                    assertThat(result.getInt(1)).isZero();
                }
                assertThatThrownBy(() -> statement.execute("INSERT INTO tarefa (projeto_id) VALUES (999999)"))
                        .isInstanceOf(java.sql.SQLException.class).hasMessageContaining("foreign key");
            }
        }
    }

    @Test
    void exigeBaselineExplicitoEPreservaDadosEIdentidadesDoEsquemaLegado() throws Exception {
        try (var database = new PostgresTestDatabase()) {
            try (var connection = database.connectToSchema()) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("legacy/schema.sql"));
            }
            var flyway = database.flyway();
            assertThatThrownBy(flyway::migrate).isInstanceOf(FlywayException.class)
                    .hasMessageContaining("non-empty");
            flyway.baseline();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            flyway.validate();
            try (var connection = database.connectToSchema(); var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("""
                        SELECT t.id, t.status, t.peso_percentual, p.nome, u.senha
                        FROM tarefa t JOIN projeto p ON p.id = t.projeto_id
                        JOIN usuario u ON u.id = t.responsavel_id
                        JOIN projeto_colaboradores pc ON pc.projeto_id = p.id AND pc.usuario_id = u.id
                        """)) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getLong(1)).isEqualTo(1);
                    assertThat(result.getString(2)).isEqualTo("VALIDADA");
                    assertThat(result.getDouble(3)).isEqualTo(50);
                    assertThat(result.getString(4)).isEqualTo("Jambu legado");
                    assertThat(result.getString(5)).isEqualTo("senha-legada");
                    assertThat(result.next()).isFalse();
                }
                try (var result = statement.executeQuery("INSERT INTO tarefa (titulo, projeto_id) VALUES ('Nova', 1) RETURNING id")) {
                    result.next();
                    assertThat(result.getLong(1)).isGreaterThan(1);
                }
                try (var result = statement.executeQuery("SELECT count(*) FROM pg_indexes WHERE schemaname = current_schema() AND indexname LIKE 'idx_%'")) {
                    result.next();
                    assertThat(result.getInt(1)).isEqualTo(4);
                }
            }
        }
    }
}
