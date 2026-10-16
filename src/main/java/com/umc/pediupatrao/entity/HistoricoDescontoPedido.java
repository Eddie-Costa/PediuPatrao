package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.time.Instant;

public class HistoricoDescontoPedido {

    private BigDecimal percentualAnterior;
    private BigDecimal valorAnterior;
    private BigDecimal totalAnterior;
    private BigDecimal percentualNovo;
    private BigDecimal valorNovo;
    private BigDecimal totalNovo;
    private String usuario;
    private String perfil;
    private Instant dataHora;

    public HistoricoDescontoPedido() {
    }

    public HistoricoDescontoPedido(BigDecimal percentualAnterior, BigDecimal valorAnterior,
            BigDecimal totalAnterior, BigDecimal percentualNovo, BigDecimal valorNovo,
            BigDecimal totalNovo, String usuario, String perfil, Instant dataHora) {
        this.percentualAnterior = percentualAnterior;
        this.valorAnterior = valorAnterior;
        this.totalAnterior = totalAnterior;
        this.percentualNovo = percentualNovo;
        this.valorNovo = valorNovo;
        this.totalNovo = totalNovo;
        this.usuario = usuario;
        this.perfil = perfil;
        this.dataHora = dataHora;
    }

    public BigDecimal getPercentualAnterior() {
        return percentualAnterior;
    }

    public void setPercentualAnterior(BigDecimal percentualAnterior) {
        this.percentualAnterior = percentualAnterior;
    }

    public BigDecimal getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(BigDecimal valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public BigDecimal getTotalAnterior() {
        return totalAnterior;
    }

    public void setTotalAnterior(BigDecimal totalAnterior) {
        this.totalAnterior = totalAnterior;
    }

    public BigDecimal getPercentualNovo() {
        return percentualNovo;
    }

    public void setPercentualNovo(BigDecimal percentualNovo) {
        this.percentualNovo = percentualNovo;
    }

    public BigDecimal getValorNovo() {
        return valorNovo;
    }

    public void setValorNovo(BigDecimal valorNovo) {
        this.valorNovo = valorNovo;
    }

    public BigDecimal getTotalNovo() {
        return totalNovo;
    }

    public void setTotalNovo(BigDecimal totalNovo) {
        this.totalNovo = totalNovo;
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

    public Instant getDataHora() {
        return dataHora;
    }

    public void setDataHora(Instant dataHora) {
        this.dataHora = dataHora;
    }
}
