package com.SgP.demo;

import com.SgP.demo.models.Tarefa;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.services.CalculadoraProgresso;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraProgressoTests {
    private final CalculadoraProgresso calculadora = new CalculadoraProgresso();

    @Test
    void projetoSemTarefasTemProgressoZero() {
        assertThat(calculadora.calcular(List.of())).isZero();
    }

    @Test
    void execucaoAguardandoValidacaoNaoContribuiNoProjeto() {
        assertThat(calculadora.calcular(List.of(tarefa("AGUARDANDO_VALIDACAO", 100.0)))).isZero();
    }

    @Test
    void tarefasSemPesoDividemRestanteComArredondamento() {
        assertThat(calculadora.calcular(List.of(tarefa("CONCLUIDA", 40.0),
                tarefa("CONCLUIDA", null), tarefa("PENDENTE", null)))).isEqualTo(70.0);
        assertThat(calculadora.calcular(List.of(tarefa("CONCLUIDA", null),
                tarefa("PENDENTE", null), tarefa("PENDENTE", null)))).isEqualTo(33.33);
    }

    @Test
    void pesosExplicitosIncompletosPreservamRegraAtual() {
        assertThat(calculadora.calcular(List.of(tarefa("CONCLUIDA", 20.0),
                tarefa("CONCLUIDA", 30.0)))).isEqualTo(50.0);
    }

    @Test
    void pesosLegadosInvalidosNaoGeramNaNENemExcedemCem() {
        assertThat(calculadora.calcular(List.of(tarefa("Validada", Double.NaN),
                tarefa("CONCLUIDA", -10.0)))).isEqualTo(100.0);
        assertThat(calculadora.calcular(List.of(tarefa("CONCLUIDA", 120.0),
                tarefa("CONCLUIDA", null)))).isEqualTo(100.0);
    }

    @Test
    void responsavelSemIdNaoPermiteExecucaoENaoCausaExcecao() {
        Tarefa tarefa = tarefa("PENDENTE", null);
        tarefa.setResponsavel(new Usuario());
        assertThat(tarefa.podeExecutar(new Usuario())).isFalse();
    }

    private Tarefa tarefa(String status, Double peso) {
        Tarefa tarefa = new Tarefa();
        tarefa.setStatus(status);
        tarefa.setPesoPercentual(peso);
        return tarefa;
    }
}
