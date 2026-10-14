package com.SgP.demo.services;

import com.SgP.demo.exceptions.DadosInvalidosException;
import com.SgP.demo.models.Usuario;
import com.SgP.demo.repositories.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository usuarios;

    public UsuarioService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    public Optional<Usuario> autenticar(String email, String senha) {
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) return Optional.empty();
        return Optional.ofNullable(usuarios.findByEmailAndSenha(email.trim(), senha));
    }

    public List<Usuario> listarColaboradores() {
        return usuarios.findByFuncaoOrderByNomeAsc("COLABORADOR");
    }

    public Usuario buscarColaborador(Long id) {
        if (id == null) return null;
        return usuarios.findById(id).filter(Usuario::isColaborador)
                .orElseThrow(() -> new DadosInvalidosException("responsavel", "Selecione um colaborador válido."));
    }

    public Usuario buscarUsuarioAtual(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) return null;
        return usuarios.findById(usuario.getId()).orElse(null);
    }

    public void exigirGestor(Usuario usuario) {
        Usuario atual = buscarUsuarioAtual(usuario);
        if (atual == null || !atual.isGestor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta operação exige um gestor.");
        }
    }
}
