package com.umc.pediupatrao.entity;

import jakarta.persistence.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pedidos")
@Entity
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private String tipoPizza;
    private String status;
    private String justificativaCancelamento;
    private Instant recebidoEm;
    private String recebidoPor;
    private Instant saidaEm;
    private String saidaPor;
    private Instant finalizadoEm;
    private String finalizadoPor;
    private Instant canceladoEm;
    private String canceladoPor;
    private List<HistoricoStatusPedido> historicoStatus = new ArrayList<>();
    private List<ItemPedido> itens = new ArrayList<>();
    private int quantidade;
    private String sabor;
    private String produtoId;
    private String produtoNome;
    private BigDecimal precoUnitario;
    private BigDecimal percentualDesconto;
    private BigDecimal valorDesconto;
    private BigDecimal valorTotal;
    @JsonIgnore
    private List<HistoricoDescontoPedido> historicoDescontos = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getTipoPizza() {
        return tipoPizza;
    }

    public void setTipoPizza(String tipoPizza) {
        this.tipoPizza = tipoPizza;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJustificativaCancelamento() {
        return justificativaCancelamento;
    }

    public void setJustificativaCancelamento(String justificativaCancelamento) {
        this.justificativaCancelamento = justificativaCancelamento;
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }

    public void setRecebidoEm(Instant recebidoEm) {
        this.recebidoEm = recebidoEm;
    }

    public String getRecebidoPor() {
        return recebidoPor;
    }

    public void setRecebidoPor(String recebidoPor) {
        this.recebidoPor = recebidoPor;
    }

    public Instant getSaidaEm() {
        return saidaEm;
    }

    public void setSaidaEm(Instant saidaEm) {
        this.saidaEm = saidaEm;
    }

    public String getSaidaPor() {
        return saidaPor;
    }

    public void setSaidaPor(String saidaPor) {
        this.saidaPor = saidaPor;
    }

    public Instant getFinalizadoEm() {
        return finalizadoEm;
    }

    public void setFinalizadoEm(Instant finalizadoEm) {
        this.finalizadoEm = finalizadoEm;
    }

    public String getFinalizadoPor() {
        return finalizadoPor;
    }

    public void setFinalizadoPor(String finalizadoPor) {
        this.finalizadoPor = finalizadoPor;
    }

    public Instant getCanceladoEm() {
        return canceladoEm;
    }

    public void setCanceladoEm(Instant canceladoEm) {
        this.canceladoEm = canceladoEm;
    }

    public String getCanceladoPor() {
        return canceladoPor;
    }

    public void setCanceladoPor(String canceladoPor) {
        this.canceladoPor = canceladoPor;
    }

    public List<HistoricoStatusPedido> getHistoricoStatus() {
        if (historicoStatus == null) {
            historicoStatus = new ArrayList<>();
        }
        return historicoStatus;
    }

    public void setHistoricoStatus(List<HistoricoStatusPedido> historicoStatus) {
        this.historicoStatus = historicoStatus == null ? new ArrayList<>() : historicoStatus;
    }

    public List<ItemPedido> getItens() {
        if (itens == null) {
            itens = new ArrayList<>();
        }
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens == null ? new ArrayList<>() : itens;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public String getSabor() {
        return sabor;
    }

    public void setSabor(String sabor) {
        this.sabor = sabor;
    }

    public String getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(String produtoId) {
        this.produtoId = produtoId;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public void setProdutoNome(String produtoNome) {
        this.produtoNome = produtoNome;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(BigDecimal percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public List<HistoricoDescontoPedido> getHistoricoDescontos() {
        if (historicoDescontos == null) {
            historicoDescontos = new ArrayList<>();
        }
        return historicoDescontos;
    }

    public void setHistoricoDescontos(List<HistoricoDescontoPedido> historicoDescontos) {
        this.historicoDescontos = historicoDescontos == null
                ? new ArrayList<>() : historicoDescontos;
    }
}
