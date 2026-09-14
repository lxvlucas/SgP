package com.SgP.demo.controllers;

import com.SgP.demo.models.Projeto;
import com.SgP.demo.models.Tarefa;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.repositories.ProjetoRepository;
import com.SgP.demo.repositories.UsuarioRepository;
import com.SgP.demo.services.TarefaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class TarefaController {
    private final ProjetoRepository projetos;
    private final UsuarioRepository usuarios;
    private final TarefaService tarefas;

    public TarefaController(ProjetoRepository projetos, UsuarioRepository usuarios, TarefaService tarefas) {
        this.projetos = projetos;
        this.usuarios = usuarios;
        this.tarefas = tarefas;
    }

    @InitBinder("tarefa")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("titulo", "descricao", "prazo", "pesoPercentual");
    }

    @GetMapping({"/projeto/{id}/tarefas", "/projeto/{id}"})
    public String verTarefas(@PathVariable Long id, Model model) {
        Projeto projeto = projetos.findById(id).orElse(null);
        if (projeto == null) return "redirect:/";
        model.addAttribute("projeto", projeto);
        model.addAttribute("tarefas", projeto.getTarefas());
        return "tarefas";
    }

    @GetMapping("/projeto/{projetoId}/tarefa/nova")
    public String novaTarefaForm(@PathVariable Long projetoId, Model model, HttpSession session) {
        if (!gestor(session)) return "redirect:/";
        Projeto projeto = projetos.findById(projetoId).orElse(null);
        if (projeto == null) return "redirect:/";
        model.addAttribute("tarefa", new Tarefa());
        return formulario(projeto, model);
    }

    @PostMapping("/projeto/{projetoId}/tarefa/salvar")
    public String salvarTarefa(@PathVariable Long projetoId, @ModelAttribute Tarefa tarefa,
            BindingResult erros, @RequestParam(required = false) Long responsavelId,
            Model model, HttpSession session) {
        if (!gestor(session)) return "redirect:/";
        Projeto projeto = projetos.findById(projetoId).orElse(null);
        if (projeto == null) return "redirect:/";
        if (!textoValido(tarefa.getTitulo()) || !textoValido(tarefa.getDescricao())) {
            erros.reject("texto", "Preencha título e descrição com até 255 caracteres cada.");
        }
        Usuario responsavel = responsavelId == null ? null : usuarios.findById(responsavelId).orElse(null);
        if (responsavelId != null && (responsavel == null || !"COLABORADOR".equals(responsavel.getFuncao()))) {
            erros.reject("responsavel", "Selecione um colaborador válido.");
        }
        Double peso = tarefa.getPesoPercentual();
        double reservado = projeto.getTarefas().stream().map(Tarefa::getPesoPercentual)
                .filter(p -> p != null && Double.isFinite(p) && p > 0).mapToDouble(Double::doubleValue).sum();
        if (peso != null && (!Double.isFinite(peso) || peso <= 0 || peso + reservado > 100.000001)) {
            erros.reject("peso", "O peso deve ser maior que zero e a soma dos pesos não pode ultrapassar 100%.");
        }
        if (erros.hasErrors()) {
            model.addAttribute("responsavelSelecionado", responsavelId);
            return formulario(projeto, model);
        }
        tarefa.setTitulo(tarefa.getTitulo().trim());
        tarefa.setDescricao(tarefa.getDescricao().trim());
        tarefa.setResponsavel(responsavel);
        tarefas.salvar(tarefa, projetoId);
        return destino(projetoId);
    }

    @PostMapping("/tarefa/{id}/executar")
    public String registrarExecucao(@PathVariable Long id, HttpSession session) {
        return destino(tarefas.registrarExecucao(id, (Usuario) session.getAttribute("usuarioLogado")));
    }

    @PostMapping("/tarefa/{id}/validar")
    public String validarTarefa(@PathVariable Long id, HttpSession session) {
        return destino(tarefas.validarTarefa(id, (Usuario) session.getAttribute("usuarioLogado")));
    }

    private String formulario(Projeto projeto, Model model) {
        model.addAttribute("projeto", projeto);
        model.addAttribute("colaboradores", usuarios.findByFuncaoOrderByNomeAsc("COLABORADOR"));
        return "nova-tarefa";
    }

    private String destino(Long projetoId) {
        return projetoId == null ? "redirect:/" : "redirect:/projeto/" + projetoId + "/tarefas";
    }

    private boolean gestor(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        return usuario != null && "GESTOR".equals(usuario.getFuncao());
    }

    private boolean textoValido(String texto) {
        return texto != null && !texto.isBlank() && texto.trim().length() <= 255;
    }
}
