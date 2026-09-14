package com.SgP.demo.controllers;

import com.SgP.demo.models.Projeto;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.repositories.ProjetoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProjetoController {
    private final ProjetoRepository projetos;

    public ProjetoController(ProjetoRepository projetos) {
        this.projetos = projetos;
    }

    @InitBinder("projeto")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("nome", "descricao");
    }

    @GetMapping("/")
    public String listarProjetos(Model model) {
        model.addAttribute("projetos", projetos.findAll());
        return "index";
    }

    @GetMapping("/projeto/novo")
    public String formNovoProjeto(Model model, HttpSession session) {
        if (!gestor(session)) return "redirect:/";
        model.addAttribute("projeto", new Projeto());
        return "novo-projeto";
    }

    @PostMapping("/projeto/salvar")
    public String salvarProjeto(@ModelAttribute Projeto projeto, BindingResult erros, HttpSession session) {
        if (!gestor(session)) return "redirect:/";
        if (!textoValido(projeto.getNome()) || !textoValido(projeto.getDescricao())) {
            erros.reject("texto", "Preencha nome e descrição com até 255 caracteres cada.");
        }
        if (erros.hasErrors()) return "novo-projeto";
        projeto.setNome(projeto.getNome().trim());
        projeto.setDescricao(projeto.getDescricao().trim());
        projetos.save(projeto);
        return "redirect:/";
    }

    @PostMapping("/projeto/{id}/excluir")
    public String excluirProjeto(@PathVariable Long id, HttpSession session) {
        if (gestor(session)) projetos.findById(id).ifPresent(projetos::delete);
        return "redirect:/";
    }

    private boolean gestor(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        return usuario != null && "GESTOR".equals(usuario.getFuncao());
    }

    private boolean textoValido(String texto) {
        return texto != null && !texto.isBlank() && texto.trim().length() <= 255;
    }
}
