package com.SgP.demo.support;

import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.MapPropertySource;

public class PostgresTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        try {
            var database = new PostgresTestDatabase();
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolatedPostgres", Map.of(
                    "spring.datasource.url", database.url(),
                    "spring.datasource.username", database.username(),
                    "spring.datasource.password", database.password(),
                    "spring.datasource.hikari.schema", database.schema(),
                    "spring.jpa.properties.hibernate.default_schema", database.schema(),
                    "spring.flyway.schemas", database.schema(),
                    "spring.flyway.default-schema", database.schema())));
            var factory = (DefaultListableBeanFactory) context.getBeanFactory();
            factory.registerSingleton("isolatedPostgresDatabase", database);
            factory.registerDisposableBean("isolatedPostgresDatabase", database::close);
            factory.registerDependentBean("isolatedPostgresDatabase", "dataSource");
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível preparar o PostgreSQL exclusivo de testes.", exception);
        }
    }
}
