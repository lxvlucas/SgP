package com.SgP.demo.services;

import com.SgP.demo.forms.ProjetoForm;
import com.SgP.demo.models.Projeto;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.repositories.ProjetoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjetoService {
    private final ProjetoRepository projetos;
    private final UsuarioService usuarios;

    public ProjetoService(ProjetoRepository projetos, UsuarioService usuarios) {
        this.projetos = projetos;
        this.usuarios = usuarios;
    }

    public List<Projeto> listar() { return projetos.findAll(); }

    public Optional<Projeto> buscar(Long id) { return projetos.findById(id); }

    public Optional<Projeto> buscarComTarefas(Long id) {
        Optional<Projeto> projeto = buscar(id);
        projeto.ifPresent(p -> p.getTarefas().size());
        return projeto;
    }

    @Transactional
    public Projeto criar(ProjetoForm formulario, Usuario autor) {
        usuarios.exigirGestor(autor);
        Projeto projeto = new Projeto();
        projeto.setNome(ValidacaoCadastro.textoObrigatorio(formulario.getNome(), "nome"));
        projeto.setDescricao(ValidacaoCadastro.textoObrigatorio(formulario.getDescricao(), "descrição"));
        return projetos.save(projeto);
    }

    @Transactional
    public void excluir(Long id, Usuario autor) {
        usuarios.exigirGestor(autor);
        projetos.buscarParaAtualizacao(id).ifPresent(projetos::delete);
    }
}
