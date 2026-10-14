package com.SgP.demo;

import com.SgP.demo.support.PostgresTestDatabase;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresTestSafetyTests {
    @Test
    void aceitaSomenteBasesExplicitamenteReservadasParaTeste() {
        assertThatCode(() -> PostgresTestDatabase.validateUrl("jdbc:postgresql://localhost:5432/sgp_test"))
                .doesNotThrowAnyException();
        assertThatCode(() -> PostgresTestDatabase.validateUrl("jdbc:postgresql://localhost/sgp_test_ci"))
                .doesNotThrowAnyException();
        for (String url : new String[] {"jdbc:postgresql://localhost/sgp_db",
                "jdbc:postgresql://localhost/postgres", "jdbc:h2:mem:sgp_test",
                "jdbc:postgresql://localhost/sgp_test?currentSchema=public",
                "jdbc:postgresql://localhost/sgp_test#extra", "jdbc:postgresql:sgp_test"}) {
            assertThatThrownBy(() -> PostgresTestDatabase.validateUrl(url))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
