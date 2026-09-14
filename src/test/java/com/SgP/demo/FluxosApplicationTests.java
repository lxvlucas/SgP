package com.SgP.demo;

import com.SgP.demo.config.DataLoader;
import com.SgP.demo.models.Projeto;
import com.SgP.demo.models.Tarefa;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.repositories.ProjetoRepository;
import com.SgP.demo.repositories.TarefaRepository;
import com.SgP.demo.repositories.UsuarioRepository;
import com.SgP.demo.services.TarefaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FluxosApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ProjetoRepository projetos;
    @Autowired TarefaRepository tarefas;
    @Autowired UsuarioRepository usuarios;
    @Autowired TarefaService service;
    @Autowired DataLoader loader;
    @Autowired EntityManager em;
    MockHttpSession gestor;
    MockHttpSession joao;
    MockHttpSession pedro;
    Projeto projeto;

    @BeforeEach
    void preparar() {
        gestor = sessao("gestor@sgp.com");
        joao = sessao("joao@sgp.com");
        pedro = sessao("pedro@sgp.com");
        projeto = new Projeto();
        projeto.setNome("Projeto de teste");
        projeto.setDescricao("Descrição do teste");
        projetos.save(projeto);
    }

    @Test
    void loginLogoutEProtecaoDasPaginas() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/projeto/salvar").param("nome", "Visitante"))
                .andExpect(redirectedUrl("/login"));
        mvc.perform(post("/fazer-login").param("email", "gestor@sgp.com").param("senha", "errada"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("inválidos")));
        var resultado = mvc.perform(post("/fazer-login").param("email", "gestor@sgp.com")
                .param("senha", "123456")).andExpect(redirectedUrl("/")).andReturn();
        MockHttpSession logado = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(get("/").session(logado)).andExpect(status().isOk());
        mvc.perform(get("/logout").session(logado)).andExpect(redirectedUrl("/login"));
        assertThat(logado.isInvalid()).isTrue();
    }

    @Test
    void linkVerTarefasAbreTelaEEnderecoAntigoTambemFunciona() throws Exception {
        String caminho = "/projeto/" + projeto.getId();
        mvc.perform(get("/").session(gestor)).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"" + caminho + "/tarefas\"")));
        mvc.perform(get(caminho + "/tarefas").session(gestor)).andExpect(status().isOk())
                .andExpect(view().name("tarefas"))
                .andExpect(content().string(containsString("Nenhuma tarefa cadastrada")));
        mvc.perform(get(caminho).session(joao)).andExpect(status().isOk()).andExpect(view().name("tarefas"));
    }

    @Test
    void cadastraProjetoETarefaComResponsavelPrazoEPeso() throws Exception {
        mvc.perform(get("/projeto/novo").session(gestor)).andExpect(status().isOk());
        mvc.perform(post("/projeto/salvar").session(gestor).param("nome", "Novo projeto")
                .param("descricao", "Teste de cadastro").param("id", projeto.getId().toString())
                .param("percentualConclusao", "100"))
                .andExpect(redirectedUrl("/"));
        Projeto novo = projetos.findAll().stream().filter(p -> "Novo projeto".equals(p.getNome())).findFirst().orElseThrow();
        assertThat(novo.getId()).isNotEqualTo(projeto.getId());
        assertThat(novo.getPercentualConclusao()).isZero();
        mvc.perform(get("/projeto/" + novo.getId() + "/tarefa/nova").session(gestor))
                .andExpect(status().isOk()).andExpect(content().string(containsString("João")));
        mvc.perform(post("/projeto/" + novo.getId() + "/tarefa/salvar").session(gestor)
                .param("titulo", "Nova tarefa").param("descricao", "Descrição")
                .param("responsavelId", usuario(joao).getId().toString())
                .param("prazo", "2027-01-10").param("pesoPercentual", "100")
                .param("status", "CONCLUIDA").param("progresso", "100"))
                .andExpect(redirectedUrl("/projeto/" + novo.getId() + "/tarefas"));
        em.flush();
        em.clear();
        Tarefa criada = tarefas.findAll().stream().filter(t -> "Nova tarefa".equals(t.getTitulo())).findFirst().orElseThrow();
        assertThat(criada.getStatus()).isEqualTo("PENDENTE");
        assertThat(criada.getProgresso()).isZero();
        assertThat(criada.getPrazo()).hasToString("2027-01-10");
        assertThat(criada.getPesoPercentual()).isEqualTo(100);
        assertThat(criada.getResponsavel().getId()).isEqualTo(usuario(joao).getId());
        assertThat(criada.getProjeto().getColaboradores()).extracting(Usuario::getId).contains(usuario(joao).getId());
    }

    @Test
    void executaValidaEAtualizaProgressoSemPeso() throws Exception {
        Tarefa primeira = tarefa("PENDENTE", null, usuario(joao));
        Tarefa segunda = tarefa("EM_ANDAMENTO", null, usuario(pedro));
        mvc.perform(post("/tarefa/" + primeira.getId() + "/executar").session(joao))
                .andExpect(redirectedUrl("/projeto/" + projeto.getId() + "/tarefas"));
        assertThat(primeira.getStatus()).isEqualTo("AGUARDANDO_VALIDACAO");
        assertThat(primeira.getProgresso()).isEqualTo(100);
        assertThat(projeto.getPercentualConclusao()).isZero();
        mvc.perform(get("/projeto/" + projeto.getId() + "/tarefas").session(gestor))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/tarefa/" + primeira.getId() + "/validar")));
        mvc.perform(post("/tarefa/" + primeira.getId() + "/validar").session(gestor)).andExpect(status().is3xxRedirection());
        assertThat(primeira.getStatus()).isEqualTo("CONCLUIDA");
        assertThat(projeto.getPercentualConclusao()).isEqualTo(50);
        mvc.perform(get("/projeto/" + projeto.getId() + "/tarefas").session(pedro))
                .andExpect(content().string(containsString("/tarefa/" + segunda.getId() + "/executar")));
        mvc.perform(post("/tarefa/" + segunda.getId() + "/executar").session(pedro)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/tarefa/" + segunda.getId() + "/validar").session(gestor)).andExpect(status().is3xxRedirection());
        em.flush();
        em.clear();
        assertThat(projetos.findById(projeto.getId()).orElseThrow().getPercentualConclusao()).isEqualTo(100);
        mvc.perform(get("/").session(gestor)).andExpect(content().string(containsString("100.0%")));
    }

    @Test
    void pesoExplicitoERestanteAutomatico() {
        tarefa("CONCLUIDA", 40.0, usuario(joao));
        Tarefa pendente = tarefa("PENDENTE", null, usuario(pedro));
        service.recalcularProgressoProjeto(projeto.getId());
        assertThat(projeto.getPercentualConclusao()).isEqualTo(40);
        service.registrarExecucao(pendente.getId(), usuario(pedro));
        service.validarTarefa(pendente.getId(), usuario(gestor));
        assertThat(projeto.getPercentualConclusao()).isEqualTo(100);
    }

    @Test
    void novaTarefaRecalculaProjetoAntesConcluido() throws Exception {
        tarefa("CONCLUIDA", null, usuario(joao));
        service.recalcularProgressoProjeto(projeto.getId());
        assertThat(projeto.getPercentualConclusao()).isEqualTo(100);
        mvc.perform(post("/projeto/" + projeto.getId() + "/tarefa/salvar").session(gestor)
                .param("titulo", "Outra tarefa").param("descricao", "Descrição"))
                .andExpect(status().is3xxRedirection());
        assertThat(projeto.getPercentualConclusao()).isEqualTo(50);
    }

    @Test
    void statusAntigosContinuamFuncionandoAposLeituraDoBanco() throws Exception {
        Tarefa antiga = tarefa("PENDENTE", null, usuario(joao));
        em.flush();
        em.createNativeQuery("update tarefa set status = 'Em Validação' where id = :id")
                .setParameter("id", antiga.getId()).executeUpdate();
        em.clear();
        mvc.perform(get("/projeto/" + projeto.getId() + "/tarefas").session(gestor))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/tarefa/" + antiga.getId() + "/validar")));
        mvc.perform(post("/tarefa/" + antiga.getId() + "/validar").session(gestor)).andExpect(status().is3xxRedirection());
        assertThat(tarefas.findById(antiga.getId()).orElseThrow().getStatus()).isEqualTo("CONCLUIDA");
        em.flush();
        em.createNativeQuery("update tarefa set status = 'Validada' where id = :id")
                .setParameter("id", antiga.getId()).executeUpdate();
        em.clear();
        service.recalcularProgressoProjeto(projeto.getId());
        assertThat(projetos.findById(projeto.getId()).orElseThrow().getPercentualConclusao()).isEqualTo(100);
    }

    @Test
    void colaboradorNaoCriaExcluiOuValidaNemExecutaTarefaDeOutro() throws Exception {
        long quantidade = projetos.count();
        Tarefa t = tarefa("PENDENTE", null, usuario(pedro));
        mvc.perform(get("/projeto/novo").session(joao)).andExpect(redirectedUrl("/"));
        mvc.perform(post("/projeto/salvar").session(joao).param("nome", "Indevido").param("descricao", "Teste"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(get("/projeto/" + projeto.getId() + "/tarefa/nova").session(joao)).andExpect(redirectedUrl("/"));
        long quantidadeTarefas = tarefas.count();
        mvc.perform(post("/projeto/" + projeto.getId() + "/tarefa/salvar").session(joao)
                .param("titulo", "Indevida").param("descricao", "Teste")).andExpect(redirectedUrl("/"));
        assertThat(tarefas.count()).isEqualTo(quantidadeTarefas);
        mvc.perform(post("/projeto/" + projeto.getId() + "/excluir").session(joao)).andExpect(redirectedUrl("/"));
        assertThat(projetos.count()).isEqualTo(quantidade);
        mvc.perform(post("/tarefa/" + t.getId() + "/executar").session(joao)).andExpect(status().is3xxRedirection());
        assertThat(t.getStatus()).isEqualTo("PENDENTE");
        t.setStatus("AGUARDANDO_VALIDACAO");
        mvc.perform(post("/tarefa/" + t.getId() + "/validar").session(joao)).andExpect(status().is3xxRedirection());
        assertThat(t.getStatus()).isEqualTo("AGUARDANDO_VALIDACAO");
        mvc.perform(get("/projeto/" + projeto.getId() + "/tarefas").session(joao))
                .andExpect(content().string(not(containsString("/tarefa/" + t.getId() + "/validar"))));
    }

    @Test
    void impedeValidacaoAntesDaExecucaoEReaberturaDeConcluida() throws Exception {
        Tarefa t = tarefa("PENDENTE", null, usuario(joao));
        mvc.perform(post("/tarefa/" + t.getId() + "/validar").session(gestor)).andExpect(status().is3xxRedirection());
        assertThat(t.getStatus()).isEqualTo("PENDENTE");
        t.setStatus("CONCLUIDA");
        service.recalcularProgressoProjeto(projeto.getId());
        mvc.perform(post("/tarefa/" + t.getId() + "/executar").session(gestor)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/tarefa/" + t.getId() + "/validar").session(gestor)).andExpect(status().is3xxRedirection());
        assertThat(t.getStatus()).isEqualTo("CONCLUIDA");
        assertThat(projeto.getPercentualConclusao()).isEqualTo(100);
    }

    @Test
    void formulariosInvalidosExibemErroSemGravar() throws Exception {
        long quantidadeProjetos = projetos.count();
        long quantidadeTarefas = tarefas.count();
        mvc.perform(post("/projeto/salvar").session(gestor).param("nome", " ").param("descricao", "Teste"))
                .andExpect(status().isOk()).andExpect(view().name("novo-projeto"))
                .andExpect(content().string(containsString("Preencha nome")));
        mvc.perform(post("/projeto/" + projeto.getId() + "/tarefa/salvar").session(gestor)
                .param("titulo", " ").param("descricao", "Teste"))
                .andExpect(status().isOk()).andExpect(view().name("nova-tarefa"));
        for (String peso : new String[]{"-1", "101", "NaN", "abc"}) {
            mvc.perform(post("/projeto/" + projeto.getId() + "/tarefa/salvar").session(gestor)
                    .param("titulo", "Teste").param("descricao", "Teste").param("pesoPercentual", peso))
                    .andExpect(status().isOk()).andExpect(view().name("nova-tarefa"));
        }
        mvc.perform(post("/projeto/" + projeto.getId() + "/tarefa/salvar").session(gestor)
                .param("titulo", "Teste").param("descricao", "Teste").param("prazo", "data inválida"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Confira o formato")));
        mvc.perform(post("/projeto/" + projeto.getId() + "/tarefa/salvar").session(gestor)
                .param("titulo", "Teste").param("descricao", "Teste").param("responsavelId", "999999"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("colaborador válido")));
        assertThat(projetos.count()).isEqualTo(quantidadeProjetos);
        assertThat(tarefas.count()).isEqualTo(quantidadeTarefas);
    }

    @Test
    void somaDosPesosNaoPodePassarDeCem() throws Exception {
        tarefa("PENDENTE", 80.0, null);
        mvc.perform(post("/projeto/" + projeto.getId() + "/tarefa/salvar").session(gestor)
                .param("titulo", "Teste").param("descricao", "Teste").param("pesoPercentual", "30"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("não pode ultrapassar 100%")));
    }

    @Test
    void excluiProjetoComTarefasEVinculosSemExcluirUsuarios() throws Exception {
        Tarefa t = tarefa("PENDENTE", null, usuario(joao));
        projeto.getColaboradores().add(usuario(joao));
        em.flush();
        mvc.perform(post("/projeto/" + projeto.getId() + "/excluir").session(gestor)).andExpect(redirectedUrl("/"));
        em.flush();
        em.clear();
        assertThat(projetos.existsById(projeto.getId())).isFalse();
        assertThat(tarefas.existsById(t.getId())).isFalse();
        assertThat(usuarios.existsById(usuario(joao).getId())).isTrue();
    }

    @Test
    void registrosAusentesRedirecionamSemErroInterno() throws Exception {
        mvc.perform(get("/projeto/999999/tarefas").session(gestor)).andExpect(redirectedUrl("/"));
        mvc.perform(get("/projeto/999999/tarefa/nova").session(gestor)).andExpect(redirectedUrl("/"));
        mvc.perform(post("/projeto/999999/tarefa/salvar").session(gestor)).andExpect(redirectedUrl("/"));
        mvc.perform(post("/tarefa/999999/executar").session(joao)).andExpect(redirectedUrl("/"));
        mvc.perform(post("/tarefa/999999/validar").session(gestor)).andExpect(redirectedUrl("/"));
        mvc.perform(post("/projeto/999999/excluir").session(gestor)).andExpect(redirectedUrl("/"));
    }

    @Test
    void inicializacaoRespeitaSenhaAlteradaEProjetosExcluidos() throws Exception {
        Usuario existente = usuarios.findByEmail("gestor@sgp.com");
        existente.setSenha("outra-senha");
        usuarios.saveAndFlush(existente);
        projetos.deleteAll();
        em.flush();
        loader.carregarDados(usuarios, projetos, tarefas, service).run();
        em.flush();
        assertThat(usuarios.findByEmail("gestor@sgp.com").getSenha()).isEqualTo("outra-senha");
        assertThat(projetos.count()).isZero();
        assertThat(tarefas.count()).isZero();
        assertThat(usuarios.count()).isEqualTo(3);
    }

    private MockHttpSession sessao(String email) {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute("usuarioLogado", usuarios.findByEmail(email));
        return sessao;
    }

    private Usuario usuario(MockHttpSession sessao) {
        return (Usuario) sessao.getAttribute("usuarioLogado");
    }

    private Tarefa tarefa(String status, Double peso, Usuario responsavel) {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Tarefa de teste");
        tarefa.setDescricao("Descrição");
        tarefa.setStatus(status);
        tarefa.setPesoPercentual(peso);
        tarefa.setResponsavel(responsavel);
        tarefa.setProjeto(projeto);
        tarefas.save(tarefa);
        projeto.getTarefas().add(tarefa);
        return tarefa;
    }
}
