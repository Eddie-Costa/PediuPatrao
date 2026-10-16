package com.umc.pediupatrao.entity;

import java.time.Instant;

public class HistoricoStatusPedido {

    private String statusAnterior;
    private String statusNovo;
    private Instant dataHora;
    private String responsavel;
    private String justificativa;

    public HistoricoStatusPedido() {
    }

    public HistoricoStatusPedido(String statusAnterior, String statusNovo, Instant dataHora,
            String responsavel, String justificativa) {
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.dataHora = dataHora;
        this.responsavel = responsavel;
        this.justificativa = justificativa;
    }

    public String getStatusAnterior() {
        return statusAnterior;
    }

    public void setStatusAnterior(String statusAnterior) {
        this.statusAnterior = statusAnterior;
    }

    public String getStatusNovo() {
        return statusNovo;
    }

    public void setStatusNovo(String statusNovo) {
        this.statusNovo = statusNovo;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public void setDataHora(Instant dataHora) {
        this.dataHora = dataHora;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}
