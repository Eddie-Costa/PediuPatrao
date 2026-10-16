package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin").hasRole("ADMIN")
                        .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/auditoria", "/auditoria/**", "/api/auditoria", "/api/auditoria/**")
                            .hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/produtos/novo", "/produtos/editar/**")
                            .hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/produtos/salvar")
                            .hasAnyRole("ADMIN", "GERENTE")

                        // Operações reservadas ao gerente:
                        .requestMatchers("/api/pedidos/*/desconto", "/api/pedidos/*/cancelamento")
                            .hasRole("GERENTE")
                        .requestMatchers(HttpMethod.POST, "/pedidos/*/desconto")
                            .hasRole("GERENTE")

                        // Consultas de pedidos:
                        .requestMatchers(HttpMethod.GET, "/pedidos", "/api/pedidos/**")
                            .hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")

                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/status")
                            .hasRole("GERENTE")
                        .requestMatchers(HttpMethod.POST, "/pedidos/*/status")
                            .hasRole("GERENTE")

                        .requestMatchers(HttpMethod.GET, "/pedidos/novo")
                            .hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.POST, "/pedidos/salvar")
                            .hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/pedidos/editar/**", "/pedidos/*/cancelar")
                            .hasRole("GERENTE")

                        .requestMatchers(HttpMethod.POST, "/clientes/salvar", "/clientes/editar/**",
                                "/api/clientes")
                            .hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/**")
                            .hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**")
                            .denyAll()

                        // GERENTE consulta clientes; ATENDENTE consulta e altera.
                        .requestMatchers("/clientes/novo", "/clientes/editar/**")
                            .hasRole("ATENDENTE")
                        .requestMatchers("/clientes", "/clientes/**", "/api/clientes", "/api/clientes/**")
                            .hasAnyRole("GERENTE", "ATENDENTE")

                        // Registro de pedido:
                        .requestMatchers(HttpMethod.POST, "/api/pedidos")
                        .hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/api/produtos", "/api/produtos/**")
                            .hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.POST, "/api/produtos", "/api/produtos/**")
                            .hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/produtos/**")
                            .hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/produtos/**")
                            .hasAnyRole("ADMIN", "GERENTE")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/"); // Redireciona para a página inicial após login bem-sucedido
        };
    }
}
