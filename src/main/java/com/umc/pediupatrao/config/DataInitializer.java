package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(
            UsuarioRepository repo,
            @Value("${app.admin.initial-password:}") String adminPassword) {
        return args -> {

            if (repo.findByUsername("admin").isEmpty()) {
                if (adminPassword.isBlank()) {
                    System.out.println("Usuário admin não criado: configure APP_ADMIN_INITIAL_PASSWORD.");
                    return;
                }

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

                Usuario user = new Usuario();
                user.setUsername("admin");
                user.setPassword(encoder.encode(adminPassword));
                user.setRole("ADMIN");

                repo.save(user);

                System.out.println("Usuário admin criado!");
            }
        };
    }
}