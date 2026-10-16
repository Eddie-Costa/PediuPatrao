package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByUsername(username)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
    }

    private UserDetails toUserDetails(Usuario usuario) {
        String role = usuario.getRole();
        if (role == null || role.isBlank()) {
            throw new UsernameNotFoundException("Usuario sem perfil de acesso");
        }

        role = role.trim().toUpperCase(Locale.ROOT);
        if (role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }
        if (!Set.of("ADMIN", "GERENTE", "ATENDENTE").contains(role)) {
            throw new UsernameNotFoundException("Perfil de acesso invalido");
        }

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(role)
                .build();
    }
}