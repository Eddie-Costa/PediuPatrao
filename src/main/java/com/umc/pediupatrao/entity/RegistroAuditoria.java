package com.umc.pediupatrao.entity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "auditoria")
public class RegistroAuditoria {

    @Id
    private String id;
    private String entidade;
    private String entidadeId;
    private String operacao;
    private Instant dataHora;
    private String usuario;
    private String perfil;
    private Map<String, Object> valoresAnteriores = new LinkedHashMap<>();
    private Map<String, Object> valoresNovos = new LinkedHashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntidade() {
        return entidade;
    }

    public void setEntidade(String entidade) {
        this.entidade = entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public void setEntidadeId(String entidadeId) {
        this.entidadeId = entidadeId;
    }

    public String getOperacao() {
        return operacao;
    }

    public void setOperacao(String operacao) {
        this.operacao = operacao;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public void setDataHora(Instant dataHora) {
        this.dataHora = dataHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public Map<String, Object> getValoresAnteriores() {
        return valoresAnteriores;
    }

    public void setValoresAnteriores(Map<String, Object> valoresAnteriores) {
        this.valoresAnteriores = valoresAnteriores == null
                ? new LinkedHashMap<>() : valoresAnteriores;
    }

    public Map<String, Object> getValoresNovos() {
        return valoresNovos;
    }

    public void setValoresNovos(Map<String, Object> valoresNovos) {
        this.valoresNovos = valoresNovos == null
                ? new LinkedHashMap<>() : valoresNovos;
    }
}
