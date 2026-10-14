package com.SgP.demo.controllers;

import com.SgP.demo.models.Usuario;
import com.SgP.demo.services.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
    private final UsuarioService usuarios;

    public LoginController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/login")
    public String abrirTelaLogin() {
        return "login";
    }

    @PostMapping("/fazer-login")
    public String fazerLogin(@RequestParam(required = false) String email,
            @RequestParam(required = false) String senha, HttpServletRequest request, Model model) {
        Usuario usuario = usuarios.autenticar(email, senha).orElse(null);
        if (usuario == null) {
            model.addAttribute("erro", "E-mail ou senha inválidos!");
            return "login";
        }
        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute("usuarioLogado", usuario);
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String fazerLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return "redirect:/login";
    }
}
