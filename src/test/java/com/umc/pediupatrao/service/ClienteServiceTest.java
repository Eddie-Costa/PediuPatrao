package com.umc.pediupatrao.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.RegistroAuditoriaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private RegistroAuditoriaRepository auditoriaRepository;

    @InjectMocks
    private ClienteService clienteService;

    @BeforeEach
    void configurarAuditoria() {
        ReflectionTestUtils.setField(clienteService, "auditoriaService",
                new AuditoriaService(auditoriaRepository));
    }

    @Test
    void registraInclusaoDeClienteSemValoresAnteriores() {
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocation -> {
                    Cliente cliente = invocation.getArgument(0);
                    cliente.setId("cliente-1");
                    return cliente;
                });

        Cliente cliente = new Cliente();
        cliente.setNome("Ana");
        cliente.setTelefone("11999999999");
        clienteService.salvar(cliente, "atendente", "ATENDENTE");

        verify(auditoriaRepository).save(argThat(registro ->
                "CLIENTE".equals(registro.getEntidade())
                        && "cliente-1".equals(registro.getEntidadeId())
                        && "INCLUSAO".equals(registro.getOperacao())
                        && registro.getValoresAnteriores().isEmpty()
                        && "Ana".equals(registro.getValoresNovos().get("nome"))
                        && "atendente".equals(registro.getUsuario())
                        && "ATENDENTE".equals(registro.getPerfil())));
    }

    @Test
    void registraValoresAnterioresENovosAoAlterarCliente() {
        Cliente existente = new Cliente();
        existente.setId("cliente-1");
        existente.setNome("Ana");
        existente.setTelefone("1111");
        when(clienteRepository.findById("cliente-1")).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente atualizado = new Cliente();
        atualizado.setId("cliente-1");
        atualizado.setNome("Ana Silva");
        atualizado.setTelefone("2222");
        clienteService.salvar(atualizado, "atendente", "ATENDENTE");

        verify(auditoriaRepository).save(argThat(registro ->
                "ALTERACAO".equals(registro.getOperacao())
                        && "Ana".equals(registro.getValoresAnteriores().get("nome"))
                        && "1111".equals(registro.getValoresAnteriores().get("telefone"))
                        && "Ana Silva".equals(registro.getValoresNovos().get("nome"))
                        && "2222".equals(registro.getValoresNovos().get("telefone"))
                        && "atendente".equals(registro.getUsuario())
                        && "ATENDENTE".equals(registro.getPerfil())));
    }
}
