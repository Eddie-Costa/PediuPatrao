package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente novoCliente(Cliente cliente, String usuario, String perfil) {
        cliente.setId(null);
        return salvar(cliente, usuario, perfil);
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente cliente, String usuario, String perfil) {
        if (cliente.getId() != null && cliente.getId().isBlank()) {
            cliente.setId(null);
        }
        Cliente existente = null;
        if (cliente.getId() != null && !cliente.getId().isBlank()) {
            existente = clienteRepository.findById(cliente.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Cliente não encontrado para atualização."));
        }

        Cliente salvo = clienteRepository.save(cliente);
        auditoriaService.registrar("CLIENTE", salvo.getId(),
                existente == null ? "INCLUSAO" : "ALTERACAO",
                existente == null ? new LinkedHashMap<>() : snapshotCliente(existente),
                snapshotCliente(salvo), usuario, perfil);
        return salvo;
    }

    private LinkedHashMap<String, Object> snapshotCliente(Cliente cliente) {
        LinkedHashMap<String, Object> valores = new LinkedHashMap<>();
        valores.put("nome", cliente.getNome());
        valores.put("telefone", cliente.getTelefone());
        valores.put("cep", cliente.getCep());
        valores.put("logradouro", cliente.getLogradouro());
        valores.put("numero", cliente.getNumero());
        valores.put("complemento", cliente.getComplemento());
        valores.put("bairro", cliente.getBairro());
        valores.put("cidade", cliente.getCidade());
        valores.put("estado", cliente.getEstado());
        valores.put("endereco", cliente.getEndereco());
        return valores;
    }

}
