package com.SgP.demo.controllers;

import com.SgP.demo.exceptions.DadosInvalidosException;
import com.SgP.demo.exceptions.RegistroNaoEncontradoException;
import com.SgP.demo.forms.TarefaForm;
import com.SgP.demo.models.Projeto;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.services.ProjetoService;
import com.SgP.demo.services.TarefaService;
import com.SgP.demo.services.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class TarefaController {
    private final ProjetoService projetos;
    private final UsuarioService usuarios;
    private final TarefaService tarefas;

    public TarefaController(ProjetoService projetos, UsuarioService usuarios, TarefaService tarefas) {
        this.projetos = projetos;
        this.usuarios = usuarios;
        this.tarefas = tarefas;
    }

    @InitBinder("tarefa")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("titulo", "descricao", "prazo", "pesoPercentual", "responsavelId");
    }

    @GetMapping({"/projeto/{id}/tarefas", "/projeto/{id}"})
    public String verTarefas(@PathVariable Long id, Model model) {
        Projeto projeto = projetos.buscarComTarefas(id).orElse(null);
        if (projeto == null) return "redirect:/";
        model.addAttribute("projeto", projeto);
        model.addAttribute("tarefas", projeto.getTarefas());
        return "tarefas";
    }

    @GetMapping("/projeto/{projetoId}/tarefa/nova")
    public String novaTarefaForm(@PathVariable Long projetoId, Model model,
            @SessionAttribute("usuarioLogado") Usuario usuario) {
        if (!usuario.isGestor()) return "redirect:/";
        model.addAttribute("tarefa", new TarefaForm());
        return formulario(projetoId, model);
    }

    @PostMapping("/projeto/{projetoId}/tarefa/salvar")
    public String salvarTarefa(@PathVariable Long projetoId, @ModelAttribute("tarefa") TarefaForm formulario,
            BindingResult erros, Model model, @SessionAttribute("usuarioLogado") Usuario usuario) {
        if (!usuario.isGestor()) return "redirect:/";
        if (!erros.hasErrors()) {
            try {
                tarefas.salvar(formulario, projetoId, usuario);
                return destino(projetoId);
            } catch (DadosInvalidosException erro) {
                erros.reject(erro.getCodigo(), erro.getMessage());
            } catch (RegistroNaoEncontradoException erro) {
                return "redirect:/";
            }
        }
        return formulario(projetoId, model);
    }

    @PostMapping("/tarefa/{id}/executar")
    public String registrarExecucao(@PathVariable Long id, @SessionAttribute("usuarioLogado") Usuario usuario) {
        return destino(tarefas.registrarExecucao(id, usuario));
    }

    @PostMapping("/tarefa/{id}/validar")
    public String validarTarefa(@PathVariable Long id, @SessionAttribute("usuarioLogado") Usuario usuario) {
        return destino(tarefas.validarTarefa(id, usuario));
    }

    private String formulario(Long projetoId, Model model) {
        Projeto projeto = projetos.buscar(projetoId).orElse(null);
        if (projeto == null) return "redirect:/";
        model.addAttribute("projeto", projeto);
        model.addAttribute("colaboradores", usuarios.listarColaboradores());
        return "nova-tarefa";
    }

    private String destino(Long projetoId) {
        return projetoId == null ? "redirect:/" : "redirect:/projeto/" + projetoId + "/tarefas";
    }
}
