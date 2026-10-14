package com.SgP.demo.services;

import com.SgP.demo.exceptions.DadosInvalidosException;
import com.SgP.demo.models.Tarefa;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraProgresso {
    private static final double LIMITE_PERCENTUAL = 100.0;
    private static final double TOLERANCIA_SOMA = 0.000001;

    public double calcular(List<Tarefa> tarefas) {
        double pesoInformado = somaPesosInformados(tarefas);
        long semPeso = tarefas.stream().filter(t -> !pesoValido(t.getPesoPercentual())).count();
        double pesoAutomatico = semPeso == 0 ? 0 : Math.max(0, LIMITE_PERCENTUAL - pesoInformado) / semPeso;
        double total = tarefas.stream().filter(Tarefa::isConcluida)
                .mapToDouble(t -> pesoValido(t.getPesoPercentual()) ? t.getPesoPercentual() : pesoAutomatico)
                .sum();
        return Math.round(Math.min(LIMITE_PERCENTUAL, total) * 100.0) / 100.0;
    }

    public void validarNovoPeso(Double peso, List<Tarefa> tarefas) {
        if (peso != null && (!pesoValido(peso)
                || peso + somaPesosInformados(tarefas) > LIMITE_PERCENTUAL + TOLERANCIA_SOMA)) {
            throw new DadosInvalidosException("peso",
                    "O peso deve ser maior que zero e a soma dos pesos não pode ultrapassar 100%.");
        }
    }

    private double somaPesosInformados(List<Tarefa> tarefas) {
        return tarefas.stream().map(Tarefa::getPesoPercentual)
                .filter(this::pesoValido).mapToDouble(Double::doubleValue).sum();
    }

    private boolean pesoValido(Double peso) {
        return peso != null && Double.isFinite(peso) && peso > 0;
    }
}
