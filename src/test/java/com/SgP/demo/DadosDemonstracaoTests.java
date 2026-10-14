package com.SgP.demo;

import com.SgP.demo.config.DataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DadosDemonstracaoTests {
    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(DataLoader.class);

    @Test
    void demonstracaoNaoEstaAtivaPorPadrao() {
        contexto.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DataLoader.class);
            assertThat(context).doesNotHaveBean("carregarDados");
        });
    }

    @Test
    void flagDesativadaNaoRegistraInicializador() {
        contexto.withPropertyValues("sgp.dados-demonstracao.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DataLoader.class);
            assertThat(context).doesNotHaveBean("carregarDados");
        });
    }
}
