package com.SgP.demo;

import com.SgP.demo.exceptions.DadosInvalidosException;
import com.SgP.demo.forms.ProjetoForm;
import com.SgP.demo.forms.TarefaForm;
import com.SgP.demo.models.Projeto;
import com.SgP.demo.models.Tarefa;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.repositories.ProjetoRepository;
import com.SgP.demo.repositories.TarefaRepository;
import com.SgP.demo.repositories.UsuarioRepository;
import com.SgP.demo.services.ProjetoService;
import com.SgP.demo.services.TarefaService;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.test.context.ContextConfiguration(initializers = com.SgP.demo.support.PostgresTestInitializer.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefatoracaoApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ProjetoRepository projetos;
    @Autowired TarefaRepository tarefas;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProjetoService projetoService;
    @Autowired TarefaService tarefaService;
    Usuario gestor;
    Usuario colaborador;
    Projeto projeto;

    @BeforeEach
    void preparar() {
        gestor = usuarios.findByEmail("gestor@sgp.com");
        colaborador = usuarios.findByEmail("joao@sgp.com");
        ProjetoForm formulario = new ProjetoForm();
        formulario.setNome("Projeto da refatoração");
        formulario.setDescricao("Verificação com transações reais");
        projeto = projetoService.criar(formulario, gestor);
    }

    @AfterEach
    void limpar() {
        projetoService.excluir(projeto.getId(), gestor);
    }

    @Test
    void servicosProtegemCadastroEExclusaoSemDependerDoController() {
        long quantidade = tarefas.count();
        assertThatThrownBy(() -> tarefaService.salvar(formulario(null), projeto.getId(), colaborador))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> projetoService.excluir(projeto.getId(), colaborador))
                .isInstanceOf(ResponseStatusException.class);
        ProjetoForm novo = new ProjetoForm();
        novo.setNome("Indevido");
        novo.setDescricao("Não deve ser salvo");
        assertThatThrownBy(() -> projetoService.criar(novo, colaborador))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(tarefas.count()).isEqualTo(quantidade);
        assertThat(projetos.existsById(projeto.getId())).isTrue();
    }

    @Test
    void funcaoRecebidaNaSessaoNaoSubstituiFuncaoPersistida() {
        Usuario forjado = new Usuario("Nome", "email", "senha", "GESTOR");
        forjado.setId(colaborador.getId());
        assertThatThrownBy(() -> tarefaService.salvar(formulario(null), projeto.getId(), forjado))
                .isInstanceOf(ResponseStatusException.class);
        Tarefa tarefa = tarefaService.salvar(formulario(null), projeto.getId(), gestor);
        tarefaService.registrarExecucao(tarefa.getId(), gestor);
        tarefaService.validarTarefa(tarefa.getId(), forjado);
        assertThat(tarefas.findById(tarefa.getId()).orElseThrow().getStatus()).isEqualTo("AGUARDANDO_VALIDACAO");
    }

    @Test
    void servicoValidaPesoTextoEResponsavelEReverteFalhas() {
        long quantidade = tarefas.count();
        for (Double peso : new Double[]{-1.0, 0.0, 101.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> tarefaService.salvar(formulario(peso), projeto.getId(), gestor))
                    .isInstanceOf(DadosInvalidosException.class);
        }
        TarefaForm invalida = formulario(null);
        invalida.setTitulo(" ");
        assertThatThrownBy(() -> tarefaService.salvar(invalida, projeto.getId(), gestor))
                .isInstanceOf(DadosInvalidosException.class);
        invalida.setTitulo("Título válido");
        invalida.setDescricao("x".repeat(256));
        assertThatThrownBy(() -> tarefaService.salvar(invalida, projeto.getId(), gestor))
                .isInstanceOf(DadosInvalidosException.class);
        invalida.setDescricao("Descrição válida");
        invalida.setResponsavelId(gestor.getId());
        assertThatThrownBy(() -> tarefaService.salvar(invalida, projeto.getId(), gestor))
                .isInstanceOf(DadosInvalidosException.class);
        invalida.setResponsavelId(Long.MAX_VALUE);
        assertThatThrownBy(() -> tarefaService.salvar(invalida, projeto.getId(), gestor))
                .isInstanceOf(DadosInvalidosException.class);
        assertThat(tarefas.count()).isEqualTo(quantidade);
        assertThat(projetos.findById(projeto.getId()).orElseThrow().getPercentualConclusao()).isZero();
    }

    @Test
    void formularioComResponsavelMalformadoExibeErroEMantemDados() throws Exception {
        long quantidade = tarefas.count();
        mvc.perform(post("/projeto/" + projeto.getId() + "/tarefa/salvar").session(sessao(gestor))
                .param("titulo", "Título preservado").param("descricao", "Descrição preservada")
                .param("responsavelId", "abc"))
                .andExpect(status().isOk()).andExpect(view().name("nova-tarefa"))
                .andExpect(content().string(containsString("Confira o formato")))
                .andExpect(model().attribute("tarefa", hasProperty("titulo", equalTo("Título preservado"))));
        assertThat(tarefas.count()).isEqualTo(quantidade);
    }

    @Test
    void formularioPreservaResponsavelDepoisDeErroDeValidacao() throws Exception {
        mvc.perform(post("/projeto/" + projeto.getId() + "/tarefa/salvar").session(sessao(gestor))
                .param("titulo", "Título").param("descricao", "Descrição")
                .param("responsavelId", colaborador.getId().toString()).param("pesoPercentual", "101"))
                .andExpect(status().isOk()).andExpect(view().name("nova-tarefa"))
                .andExpect(content().string(containsString("value=\"" + colaborador.getId() + "\" selected=\"selected\"")));
    }

    @Test
    void detalheRenderizaTarefasEResponsavelSemOpenInView() throws Exception {
        TarefaForm formulario = formulario(null);
        formulario.setResponsavelId(colaborador.getId());
        tarefaService.salvar(formulario, projeto.getId(), gestor);
        mvc.perform(get("/projeto/" + projeto.getId() + "/tarefas").session(sessao(gestor)))
                .andExpect(status().isOk()).andExpect(content().string(containsString(colaborador.getNome())));
    }

    @Test
    void loginTrocaIdDaSessaoERejeitaCredenciaisAusentes() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        String idAnterior = sessao.getId();
        mvc.perform(post("/fazer-login").session(sessao).param("email", " gestor@sgp.com ")
                .param("senha", "123456")).andExpect(redirectedUrl("/"));
        assertThat(sessao.getId()).isNotEqualTo(idAnterior);
        mvc.perform(post("/fazer-login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("inválidos")));
    }

    @Test
    void interceptorNaoCriaSessaoParaVisitanteERejeitaObjetoInvalido() throws Exception {
        var resultado = mvc.perform(get("/")).andExpect(redirectedUrl("/login")).andReturn();
        assertThat(resultado.getRequest().getSession(false)).isNull();
        MockHttpSession invalida = new MockHttpSession();
        invalida.setAttribute("usuarioLogado", "não é um usuário");
        mvc.perform(get("/").session(invalida)).andExpect(redirectedUrl("/login"));
    }

    @Test
    void cadastrosConcorrentesNaoUltrapassamPesoTotal() throws Exception {
        Callable<Boolean> salvar = () -> {
            try {
                tarefaService.salvar(formulario(60.0), projeto.getId(), gestor);
                return true;
            } catch (DadosInvalidosException erro) {
                assertThat(erro.getCodigo()).isEqualTo("peso");
                return false;
            }
        };
        assertThat(simultaneamente(salvar, salvar)).containsExactlyInAnyOrder(true, false);
        List<Tarefa> gravadas = tarefas.findByProjetoIdOrderByIdAsc(projeto.getId());
        assertThat(gravadas).hasSize(1);
        assertThat(gravadas.getFirst().getPesoPercentual()).isEqualTo(60.0);
    }

    @Test
    void validacoesConcorrentesNaoPerdemProgresso() throws Exception {
        Tarefa primeira = tarefaService.salvar(formulario(null), projeto.getId(), gestor);
        Tarefa segunda = tarefaService.salvar(formulario(null), projeto.getId(), gestor);
        tarefaService.registrarExecucao(primeira.getId(), gestor);
        tarefaService.registrarExecucao(segunda.getId(), gestor);
        assertThat(simultaneamente(() -> tarefaService.validarTarefa(primeira.getId(), gestor),
                () -> tarefaService.validarTarefa(segunda.getId(), gestor)))
                .containsOnly(projeto.getId());
        assertThat(projetos.findById(projeto.getId()).orElseThrow().getPercentualConclusao()).isEqualTo(100.0);
        assertThat(tarefas.findByProjetoIdOrderByIdAsc(projeto.getId())).allMatch(Tarefa::isConcluida);
    }

    private TarefaForm formulario(Double peso) {
        TarefaForm formulario = new TarefaForm();
        formulario.setTitulo("Tarefa de teste");
        formulario.setDescricao("Descrição de teste");
        formulario.setPesoPercentual(peso);
        return formulario;
    }

    private MockHttpSession sessao(Usuario usuario) {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute("usuarioLogado", usuario);
        return sessao;
    }

    private <T> List<T> simultaneamente(Callable<T> primeira, Callable<T> segunda) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            var resultado1 = executor.submit(() -> aguardarEExecutar(primeira, prontas, iniciar));
            var resultado2 = executor.submit(() -> aguardarEExecutar(segunda, prontas, iniciar));
            assertThat(prontas.await(5, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            return List.of(resultado1.get(15, TimeUnit.SECONDS), resultado2.get(15, TimeUnit.SECONDS));
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    private <T> T aguardarEExecutar(Callable<T> operacao, CountDownLatch prontas, CountDownLatch iniciar) throws Exception {
        prontas.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Operação concorrente não iniciada.");
        return operacao.call();
    }
}
