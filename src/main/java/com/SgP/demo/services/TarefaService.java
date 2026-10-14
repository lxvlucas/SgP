package com.SgP.demo.services;

import com.SgP.demo.exceptions.RegistroNaoEncontradoException;
import com.SgP.demo.forms.TarefaForm;
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
    private final UsuarioService usuarios;
    private final CalculadoraProgresso progresso;

    public TarefaService(TarefaRepository tarefas, ProjetoRepository projetos,
            UsuarioService usuarios, CalculadoraProgresso progresso) {
        this.tarefas = tarefas;
        this.projetos = projetos;
        this.usuarios = usuarios;
        this.progresso = progresso;
    }

    public Tarefa salvar(TarefaForm formulario, Long projetoId, Usuario autor) {
        usuarios.exigirGestor(autor);
        Projeto projeto = buscarProjetoParaAtualizacao(projetoId);
        String titulo = ValidacaoCadastro.textoObrigatorio(formulario.getTitulo(), "título");
        String descricao = ValidacaoCadastro.textoObrigatorio(formulario.getDescricao(), "descrição");
        Usuario responsavel = usuarios.buscarColaborador(formulario.getResponsavelId());
        progresso.validarNovoPeso(formulario.getPesoPercentual(), tarefas.findByProjetoIdOrderByIdAsc(projetoId));

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(descricao);
        tarefa.setPrazo(formulario.getPrazo());
        tarefa.setPesoPercentual(formulario.getPesoPercentual());
        tarefa.setResponsavel(responsavel);
        projeto.adicionarTarefa(tarefa);
        tarefas.save(tarefa);
        atualizarProgresso(projeto);
        return tarefa;
    }

    public Long registrarExecucao(Long tarefaId, Usuario usuario) {
        Projeto projeto = buscarProjetoDaTarefaParaAtualizacao(tarefaId);
        if (projeto == null) return null;
        Tarefa tarefa = tarefas.findById(tarefaId).orElse(null);
        if (tarefa == null) return null;
        tarefa.registrarExecucao(usuarios.buscarUsuarioAtual(usuario));
        return projeto.getId();
    }

    public Long validarTarefa(Long tarefaId, Usuario usuario) {
        Projeto projeto = buscarProjetoDaTarefaParaAtualizacao(tarefaId);
        if (projeto == null) return null;
        Tarefa tarefa = tarefas.findById(tarefaId).orElse(null);
        if (tarefa == null) return null;
        if (tarefa.validar(usuarios.buscarUsuarioAtual(usuario))) atualizarProgresso(projeto);
        return projeto.getId();
    }

    public void recalcularProgressoProjeto(Long projetoId) {
        atualizarProgresso(buscarProjetoParaAtualizacao(projetoId));
    }

    private Projeto buscarProjetoParaAtualizacao(Long projetoId) {
        return projetos.buscarParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
    }

    private Projeto buscarProjetoDaTarefaParaAtualizacao(Long tarefaId) {
        return tarefas.buscarProjetoId(tarefaId).flatMap(projetos::buscarParaAtualizacao).orElse(null);
    }

    private void atualizarProgresso(Projeto projeto) {
        projeto.setPercentualConclusao(progresso.calcular(tarefas.findByProjetoIdOrderByIdAsc(projeto.getId())));
    }
}
