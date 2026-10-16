package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.repository.RegistroAuditoriaRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private final RegistroAuditoriaRepository auditoriaRepository;

    public AuditoriaService(RegistroAuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(String entidade, String entidadeId, String operacao,
            Map<String, Object> valoresAnteriores, Map<String, Object> valoresNovos,
            String usuario, String perfil) {
        if (usuario == null || usuario.isBlank() || perfil == null || perfil.isBlank()) {
            throw new IllegalArgumentException("Usuário e perfil são obrigatórios para registrar auditoria.");
        }

        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setOperacao(operacao);
        registro.setDataHora(Instant.now());
        registro.setUsuario(usuario);
        registro.setPerfil(perfil);
        registro.setValoresAnteriores(copiar(valoresAnteriores));
        registro.setValoresNovos(copiar(valoresNovos));
        auditoriaRepository.save(registro);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<RegistroAuditoria> listarTodos() {
        return auditoriaRepository.findAllByOrderByDataHoraDesc();
    }

    private Map<String, Object> copiar(Map<String, Object> valores) {
        return valores == null ? new LinkedHashMap<>() : new LinkedHashMap<>(valores);
    }
}
