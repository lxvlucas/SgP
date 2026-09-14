package com.SgP.demo.services;

import com.SgP.demo.models.Projeto;
import com.SgP.demo.models.Tarefa;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.repositories.ProjetoRepository;
import com.SgP.demo.repositories.TarefaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TarefaService {
    private final TarefaRepository tarefas;
    private final ProjetoRepository projetos;

    public TarefaService(TarefaRepository tarefas, ProjetoRepository projetos) {
        this.tarefas = tarefas;
        this.projetos = projetos;
    }

    public void salvar(Tarefa tarefa, Long projetoId) {
        Projeto projeto = projetos.findById(projetoId).orElseThrow();
        // Carrega a coleção antes do INSERT para não duplicar a tarefa no cálculo.
        var lista = projeto.getTarefas();
        lista.size();
        tarefa.setId(null);
        tarefa.setProjeto(projeto);
        tarefa.setStatus("PENDENTE");
        tarefa.setProgresso(0.0);
        tarefas.save(tarefa);
        lista.add(tarefa);
        if (tarefa.getResponsavel() != null && projeto.getColaboradores().stream()
                .noneMatch(u -> u.getId().equals(tarefa.getResponsavel().getId()))) {
            projeto.getColaboradores().add(tarefa.getResponsavel());
        }
        recalcularProgressoProjeto(projetoId);
    }

    public Long registrarExecucao(Long tarefaId, Usuario usuario) {
        Tarefa tarefa = tarefas.findById(tarefaId).orElse(null);
        if (tarefa == null) return null;
        if (tarefa.podeExecutar(usuario)) {
            tarefa.setStatus("AGUARDANDO_VALIDACAO");
            tarefa.setProgresso(100.0);
        }
        return tarefa.getProjeto().getId();
    }

    public Long validarTarefa(Long tarefaId, Usuario usuario) {
        Tarefa tarefa = tarefas.findById(tarefaId).orElse(null);
        if (tarefa == null) return null;
        if (usuario != null && "GESTOR".equals(usuario.getFuncao())
                && "AGUARDANDO_VALIDACAO".equals(tarefa.getStatus())) {
            tarefa.setStatus("CONCLUIDA");
            tarefa.setProgresso(100.0);
            recalcularProgressoProjeto(tarefa.getProjeto().getId());
        }
        return tarefa.getProjeto().getId();
    }

    public void recalcularProgressoProjeto(Long projetoId) {
        Projeto projeto = projetos.findById(projetoId).orElseThrow();
        var lista = projeto.getTarefas();
        double pesoInformado = lista.stream().filter(t -> pesoValido(t.getPesoPercentual()))
                .mapToDouble(Tarefa::getPesoPercentual).sum();
        long semPeso = lista.stream().filter(t -> !pesoValido(t.getPesoPercentual())).count();
        // Tarefas antigas não têm peso: dividem igualmente o percentual restante.
        double pesoAutomatico = semPeso == 0 ? 0 : Math.max(0, 100 - pesoInformado) / semPeso;
        double total = lista.stream().filter(t -> "CONCLUIDA".equals(t.getStatus()))
                .mapToDouble(t -> pesoValido(t.getPesoPercentual()) ? t.getPesoPercentual() : pesoAutomatico)
                .sum();
        projeto.setPercentualConclusao(Math.round(Math.min(100, total) * 100.0) / 100.0);
    }

    private boolean pesoValido(Double peso) {
        return peso != null && Double.isFinite(peso) && peso > 0;
    }
}
