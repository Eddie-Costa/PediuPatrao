package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record FiltroDashboard(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            String status,
            String tipoProduto,
            String produtoId) {
    }

    public record PeriodoConsulta(LocalDate inicio, LocalDate fim) {
    }

    public record RespostaDashboard<T>(PeriodoConsulta periodo, Instant atualizadoEm, T dados) {
    }

    public record ResumoVendas(
            BigDecimal faturamento,
            long totalPedidos,
            long pedidosValidos,
            long pedidosCancelados,
            BigDecimal ticketMedio,
            long itensVendidos,
            BigDecimal totalDescontos,
            BigDecimal taxaCancelamento,
            Comparativo comparativo,
            QualidadeDados qualidadeDados) {
    }

    public record Comparativo(
            PeriodoConsulta periodoAnterior,
            BigDecimal faturamentoAnterior,
            long pedidosValidosAnterior,
            BigDecimal variacaoFaturamento,
            BigDecimal variacaoPedidos) {
    }

    public record QualidadeDados(long pedidosSemData, long pedidosStatusIndefinido, long pedidosSemValor) {
    }

    public record SerieVendas(String agrupamento, List<VendaPeriodo> pontos) {
    }

    public record VendaPeriodo(LocalDate inicio, String rotulo, BigDecimal faturamento, long pedidos,
            BigDecimal ticketMedio) {
    }

    public record VendaHorario(int hora, String rotulo, long pedidos, BigDecimal faturamento) {
    }

    public record VendaTipo(String tipoProduto, long quantidade, BigDecimal faturamento,
            BigDecimal participacao) {
    }

    public record SituacaoPedidos(long total, long emAndamento, Long tempoMedioAtendimentoMinutos,
            List<StatusQuantidade> status) {
    }

    public record StatusQuantidade(String status, String rotulo, long quantidade, BigDecimal percentual) {
    }

    public record ProdutoRanking(int posicao, String produtoId, String produtoNome, String tipoProduto,
            long quantidade, BigDecimal faturamento, BigDecimal participacao) {
    }

    public record ListaPedidos(int pagina, int tamanho, long totalElementos, int totalPaginas,
            List<PedidoResumo> itens) {
    }

    public record PedidoResumo(String id, Instant recebidoEm, String status, String statusRotulo,
            String produtos, int quantidadeItens, BigDecimal percentualDesconto, BigDecimal valor) {
    }

    public record OpcoesFiltro(List<OpcaoFiltro> status, List<String> tiposProduto,
            List<ProdutoFiltro> produtos, List<OpcaoFiltro> agrupamentos, PeriodoConsulta periodoPadrao) {
    }

    public record OpcaoFiltro(String valor, String rotulo) {
    }

    public record ProdutoFiltro(String id, String nome, String tipo) {
    }
}
