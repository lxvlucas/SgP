package com.SgP.demo.controllers;

import com.SgP.demo.exceptions.DadosInvalidosException;
import com.SgP.demo.forms.ProjetoForm;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.services.ProjetoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProjetoController {
    private final ProjetoService projetos;

    public ProjetoController(ProjetoService projetos) {
        this.projetos = projetos;
    }

    @InitBinder("projeto")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("nome", "descricao");
    }

    @GetMapping("/")
    public String listarProjetos(Model model) {
        model.addAttribute("projetos", projetos.listar());
        return "index";
    }

    @GetMapping("/projeto/novo")
    public String formNovoProjeto(Model model, @SessionAttribute("usuarioLogado") Usuario usuario) {
        if (!usuario.isGestor()) return "redirect:/";
        model.addAttribute("projeto", new ProjetoForm());
        return "novo-projeto";
    }

    @PostMapping("/projeto/salvar")
    public String salvarProjeto(@ModelAttribute("projeto") ProjetoForm formulario, BindingResult erros,
            @SessionAttribute("usuarioLogado") Usuario usuario) {
        if (!usuario.isGestor()) return "redirect:/";
        if (erros.hasErrors()) return "novo-projeto";
        try {
            projetos.criar(formulario, usuario);
        } catch (DadosInvalidosException erro) {
            erros.reject(erro.getCodigo(), erro.getMessage());
            return "novo-projeto";
        }
        return "redirect:/";
    }

    @PostMapping("/projeto/{id}/excluir")
    public String excluirProjeto(@PathVariable Long id, @SessionAttribute("usuarioLogado") Usuario usuario) {
        if (usuario.isGestor()) projetos.excluir(id, usuario);
        return "redirect:/";
    }
}
