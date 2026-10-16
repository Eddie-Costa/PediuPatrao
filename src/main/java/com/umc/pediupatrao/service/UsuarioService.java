package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Locale;
import java.util.Set;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    public Usuario salvarUsuario(Usuario usuario) {
        usuario.setId(null);
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username obrigatorio.");
        }
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("Senha obrigatoria.");
        }
        usuario.setRole(validarRole(usuario.getRole()));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        return usuarioRepository.save(usuario);
    }

    // Deleta um usuário pelo ID
    public void deletarUsuario(String id, String usuarioAutenticado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));
        if (usuario.getUsername().equals(usuarioAutenticado)) {
            throw new AccessDeniedException("Nao e permitido excluir a propria conta.");
        }
        usuarioRepository.deleteById(id);
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário usuários
    public void atualizarUsuario(String id, Usuario usuarioAtualizado, String usuarioAutenticado) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));
        boolean proprioUsuario = usuarioExistente.getUsername().equals(usuarioAutenticado);
        if (proprioUsuario && usuarioAtualizado.getRole() != null
                && !usuarioAtualizado.getRole().isBlank()
                && !validarRole(usuarioAtualizado.getRole()).equals(validarRole(usuarioExistente.getRole()))) {
            throw new AccessDeniedException("Nao e permitido alterar o proprio perfil de acesso.");
        }
        boolean usernameInformado = usuarioAtualizado.getUsername() != null
                && !usuarioAtualizado.getUsername().isBlank();
        if (proprioUsuario && usernameInformado
                && !usuarioExistente.getUsername().equals(usuarioAtualizado.getUsername().trim())) {
            throw new AccessDeniedException("Nao e permitido alterar o proprio username.");
        }
        if (!proprioUsuario && !usernameInformado) {
            throw new IllegalArgumentException("Username obrigatorio.");
        }
        if (usernameInformado) {
            usuarioExistente.setUsername(usuarioAtualizado.getUsername().trim());
        }
        if (usuarioAtualizado.getRole() != null && !usuarioAtualizado.getRole().isBlank()) {
            usuarioExistente.setRole(validarRole(usuarioAtualizado.getRole()));
        }

        // Só atualiza a senha se uma nova foi informada
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }

        usuarioRepository.save(usuarioExistente);
    }

    private String validarRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Perfil de acesso obrigatorio.");
        }

        String perfil = role.trim().toUpperCase(Locale.ROOT);
        if (perfil.startsWith("ROLE_")) {
            perfil = perfil.substring("ROLE_".length());
        }
        if (!Set.of("ADMIN", "GERENTE", "ATENDENTE").contains(perfil)) {
            throw new IllegalArgumentException("Perfil de acesso invalido.");
        }
        return perfil;
    }
}
