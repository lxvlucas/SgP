package com.SgP.demo.config;

import com.SgP.demo.models.Projeto;
import com.SgP.demo.models.Tarefa;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.repositories.ProjetoRepository;
import com.SgP.demo.repositories.TarefaRepository;
import com.SgP.demo.repositories.UsuarioRepository;
import com.SgP.demo.services.TarefaService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "sgp.dados-demonstracao.enabled", havingValue = "true")
public class DataLoader {
    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    public CommandLineRunner carregarDados(UsuarioRepository usuarios, ProjetoRepository projetos,
            TarefaRepository tarefas, TarefaService tarefaService) {
        return args -> {
            boolean primeiraExecucao = usuarios.count() == 0 && projetos.count() == 0;
            garantirUsuario(usuarios, "Gestor Chefão", "gestor@sgp.com", "GESTOR");
            Usuario joao = garantirUsuario(usuarios, "João Operário", "joao@sgp.com", "COLABORADOR");
            Usuario pedro = garantirUsuario(usuarios, "Pedro Novato", "pedro@sgp.com", "COLABORADOR");

            // Não recria projetos de exemplo depois que forem excluídos.
            if (primeiraExecucao) {
                Projeto servidor = criarProjeto(projetos, "Manutenção do Servidor Central",
                        "Atualização de hardware e software do servidor.", joao, pedro);
                criarTarefa(tarefas, servidor, joao, "Comprar cabos de rede",
                        "Comprar 50m de cabo CAT6", "AGUARDANDO_VALIDACAO");
                criarTarefa(tarefas, servidor, pedro, "Formatar HDs antigos",
                        "Limpeza profunda antes do descarte", "PENDENTE");

                Projeto rede = criarProjeto(projetos, "Revisão da Rede Elétrica",
                        "Troca de cabeamento no setor sul.", joao);
                criarTarefa(tarefas, rede, joao, "Mapear disjuntores",
                        "Anotar qual disjuntor desliga qual sala", "AGUARDANDO_VALIDACAO");

                Projeto antivirus = criarProjeto(projetos, "Atualização de Antivírus",
                        "Instalação do novo pacote de segurança.", pedro);
                criarTarefa(tarefas, antivirus, pedro, "Instalar licenças",
                        "Ativar as chaves nas máquinas do RH", "EM_ANDAMENTO");
                log.info("Dados iniciais de projetos e tarefas carregados.");
            }
            for (Projeto projeto : projetos.findAll()) {
                tarefaService.recalcularProgressoProjeto(projeto.getId());
            }
        };
    }

    private Usuario garantirUsuario(UsuarioRepository usuarios, String nome, String email, String funcao) {
        Usuario existente = usuarios.findByEmail(email);
        return existente != null ? existente : usuarios.save(new Usuario(nome, email, "123456", funcao));
    }

    private Projeto criarProjeto(ProjetoRepository projetos, String nome, String descricao, Usuario... equipe) {
        Projeto projeto = new Projeto();
        projeto.setNome(nome);
        projeto.setDescricao(descricao);
        projeto.getColaboradores().addAll(List.of(equipe));
        return projetos.save(projeto);
    }

    private void criarTarefa(TarefaRepository tarefas, Projeto projeto, Usuario responsavel,
            String titulo, String descricao, String status) {
        Tarefa tarefa = new Tarefa();
        tarefa.setProjeto(projeto);
        tarefa.setResponsavel(responsavel);
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(descricao);
        tarefa.setStatus(status);
        if ("AGUARDANDO_VALIDACAO".equals(status) || "CONCLUIDA".equals(status)) tarefa.setProgresso(100.0);
        tarefas.save(tarefa);
    }
}
