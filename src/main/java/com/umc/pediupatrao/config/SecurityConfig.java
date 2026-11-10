package com.umc.pediupatrao.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.pediupatrao.dto.ErroApi;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final RequestMatcher API_DASHBOARD = new AntPathRequestMatcher("/api/dashboard/**");

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin").hasRole("ADMIN")
                        .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/auditoria", "/auditoria/**", "/api/auditoria", "/api/auditoria/**")
                            .hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/dashboard", "/api/dashboard/**")
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
                .exceptionHandling(erros -> erros
                        .defaultAuthenticationEntryPointFor(
                                (request, response, excecao) -> escreverErroJson(objectMapper, request, response,
                                        HttpStatus.UNAUTHORIZED, "Autenticação necessária para acessar o dashboard."),
                                API_DASHBOARD)
                        .defaultAccessDeniedHandlerFor(
                                (request, response, excecao) -> escreverErroJson(objectMapper, request, response,
                                        HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para acessar o dashboard."),
                                API_DASHBOARD)
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

    private void escreverErroJson(ObjectMapper objectMapper, HttpServletRequest request,
            HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErroApi.de(status, mensagem, request.getRequestURI()));
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/"); // Redireciona para a página inicial após login bem-sucedido
        };
    }
}
