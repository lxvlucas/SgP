package com.SgP.demo.config;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.SgP.demo.models.Projeto;
import com.SgP.demo.models.Tarefa;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.repositories.ProjetoRepository;
import com.SgP.demo.repositories.TarefaRepository;
import com.SgP.demo.repositories.UsuarioRepository;
import com.SgP.demo.services.TarefaService;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner carregarDados(
            UsuarioRepository usuarioRepository,
            ProjetoRepository projetoRepository,
            TarefaRepository tarefaRepository,
            TarefaService tarefaService) {
        
        return args -> {
            boolean primeiraExecucao = usuarioRepository.count() == 0 && projetoRepository.count() == 0;
            // ==========================================
            // 1. CRIAÇÃO DOS USUÁRIOS (Verificando se já existem)
            // ==========================================
            Usuario gestor = usuarioRepository.findByEmail("gestor@sgp.com");
            if (gestor == null) {
                gestor = new Usuario("Gestor Chefão", "gestor@sgp.com", "123456", "GESTOR");
                gestor = usuarioRepository.save(gestor);
            }

            Usuario joao = usuarioRepository.findByEmail("joao@sgp.com");
            if (joao == null) {
                joao = new Usuario("João Operário", "joao@sgp.com", "123456", "COLABORADOR");
                joao = usuarioRepository.save(joao);
            }

            Usuario pedro = usuarioRepository.findByEmail("pedro@sgp.com");
            if (pedro == null) {
                pedro = new Usuario("Pedro Novato", "pedro@sgp.com", "123456", "COLABORADOR");
                pedro = usuarioRepository.save(pedro);
            }

            // ==========================================
            // 2. CRIAÇÃO DOS PROJETOS E TAREFAS
            // ==========================================
            // Não recria os exemplos quando o usuário exclui todos os projetos.
            if (primeiraExecucao) {
                
                // --- PROJETO 1: JOÃO E PEDRO JUNTOS ---
                Projeto proj1 = new Projeto();
                proj1.setNome("Manutenção do Servidor Central");
                proj1.setDescricao("Atualização de hardware e software do servidor.");
                proj1.getColaboradores().addAll(Arrays.asList(joao, pedro)); // Adiciona os dois
                projetoRepository.save(proj1);

                // Tarefa do João no Projeto 1 (Aguardando Validação do Gestor)
                Tarefa t1 = new Tarefa();
                t1.setTitulo("Comprar cabos de rede");
                t1.setDescricao("Comprar 50m de cabo CAT6");
                t1.setResponsavel(joao);
                t1.setProjeto(proj1);
                t1.setStatus("AGUARDANDO_VALIDACAO");
                tarefaRepository.save(t1);

                // Tarefa do Pedro no Projeto 1 (Normal)
                Tarefa t2 = new Tarefa();
                t2.setTitulo("Formatar HDs antigos");
                t2.setDescricao("Limpeza profunda antes do descarte");
                t2.setResponsavel(pedro);
                t2.setProjeto(proj1);
                t2.setStatus("PENDENTE");
                tarefaRepository.save(t2);

                // --- PROJETO 2: SOMENTE JOÃO ---
                Projeto proj2 = new Projeto();
                proj2.setNome("Revisão da Rede Elétrica");
                proj2.setDescricao("Troca de cabeamento no setor sul.");
                proj2.getColaboradores().add(joao); // Somente João
                projetoRepository.save(proj2);

                // Tarefa do João no Projeto 2
                Tarefa t3 = new Tarefa();
                t3.setTitulo("Mapear disjuntores");
                t3.setDescricao("Anotar qual disjuntor desliga qual sala");
                t3.setResponsavel(joao);
                t3.setProjeto(proj2);
                t3.setStatus("AGUARDANDO_VALIDACAO");
                tarefaRepository.save(t3);

                // --- PROJETO 3: SOMENTE PEDRO ---
                Projeto proj3 = new Projeto();
                proj3.setNome("Atualização de Antivírus");
                proj3.setDescricao("Instalação do novo pacote de segurança.");
                proj3.getColaboradores().add(pedro); // Somente Pedro
                projetoRepository.save(proj3);

                // Tarefa do Pedro no Projeto 3
                Tarefa t4 = new Tarefa();
                t4.setTitulo("Instalar licenças");
                t4.setDescricao("Ativar as chaves nas máquinas do RH");
                t4.setResponsavel(pedro);
                t4.setProjeto(proj3);
                t4.setStatus("EM_ANDAMENTO");
                tarefaRepository.save(t4);

                System.out.println("Dados iniciais de projetos e tarefas carregados com sucesso.");
            }
            // Corrige também o progresso de projetos gravados pelo fluxo antigo.
            for (Projeto projeto : projetoRepository.findAll()) {
                tarefaService.recalcularProgressoProjeto(projeto.getId());
            }
        };
    }
}
