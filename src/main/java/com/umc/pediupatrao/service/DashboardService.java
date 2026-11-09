package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardDtos.Comparativo;
import com.umc.pediupatrao.dto.DashboardDtos.FiltroDashboard;
import com.umc.pediupatrao.dto.DashboardDtos.ListaPedidos;
import com.umc.pediupatrao.dto.DashboardDtos.OpcaoFiltro;
import com.umc.pediupatrao.dto.DashboardDtos.OpcoesFiltro;
import com.umc.pediupatrao.dto.DashboardDtos.PedidoResumo;
import com.umc.pediupatrao.dto.DashboardDtos.PeriodoConsulta;
import com.umc.pediupatrao.dto.DashboardDtos.ProdutoFiltro;
import com.umc.pediupatrao.dto.DashboardDtos.ProdutoRanking;
import com.umc.pediupatrao.dto.DashboardDtos.QualidadeDados;
import com.umc.pediupatrao.dto.DashboardDtos.RespostaDashboard;
import com.umc.pediupatrao.dto.DashboardDtos.ResumoVendas;
import com.umc.pediupatrao.dto.DashboardDtos.SerieVendas;
import com.umc.pediupatrao.dto.DashboardDtos.SituacaoPedidos;
import com.umc.pediupatrao.dto.DashboardDtos.StatusQuantidade;
import com.umc.pediupatrao.dto.DashboardDtos.VendaHorario;
import com.umc.pediupatrao.dto.DashboardDtos.VendaPeriodo;
import com.umc.pediupatrao.dto.DashboardDtos.VendaTipo;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class DashboardService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int PERIODO_PADRAO_DIAS = 30;
    private static final int PERIODO_MAXIMO_DIAS = 366;
    private static final int LIMITE_MAXIMO_RANKING = 50;
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String CANCELADO = "CANCELADO";
    private static final String FINALIZADO = "FINALIZADO";
    private static final String INDEFINIDO = "INDEFINIDO";
    private static final String NAO_INFORMADO = "Não informado";
    private static final Set<String> STATUS_EM_ANDAMENTO = Set.of(
            "RECEBIDO", "EM_PREPARACAO", "PRONTO", "SAIU_PARA_ENTREGA", "RETIRADO");
    private static final List<OpcaoFiltro> STATUS = List.of(
            new OpcaoFiltro("RECEBIDO", "Recebido"),
            new OpcaoFiltro("EM_PREPARACAO", "Em preparação"),
            new OpcaoFiltro("PRONTO", "Pronto"),
            new OpcaoFiltro("SAIU_PARA_ENTREGA", "Saiu para entrega"),
            new OpcaoFiltro("RETIRADO", "Retirado"),
            new OpcaoFiltro(FINALIZADO, "Finalizado"),
            new OpcaoFiltro(CANCELADO, "Cancelado"));
    private static final Map<String, String> ROTULOS_STATUS = STATUS.stream()
            .collect(Collectors.toUnmodifiableMap(OpcaoFiltro::valor, OpcaoFiltro::rotulo));

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ProdutoService produtoService;

    public RespostaDashboard<ResumoVendas> resumo(FiltroDashboard filtro) {
        Consulta consulta = resolver(filtro);
        List<Pedido> pedidos = carregar(consulta);
        Consulta anterior = consulta.periodoAnterior();
        List<Pedido> pedidosAnteriores = carregar(anterior).stream().filter(this::valido).toList();
        List<Pedido> validos = pedidos.stream().filter(this::valido).toList();

        BigDecimal faturamento = somarValores(validos, consulta);
        BigDecimal faturamentoAnterior = somarValores(pedidosAnteriores, anterior);
        long cancelados = pedidos.stream().filter(pedido -> CANCELADO.equals(statusDe(pedido))).count();
        long itensVendidos = validos.stream()
                .flatMap(pedido -> itensConsiderados(pedido, consulta).stream())
                .mapToLong(ItemPedido::getQuantidade)
                .sum();
        BigDecimal descontos = validos.stream()
                .map(pedido -> descontoPedido(pedido, consulta))
                .reduce(ZERO, BigDecimal::add);

        Comparativo comparativo = new Comparativo(
                anterior.periodo(),
                faturamentoAnterior,
                pedidosAnteriores.size(),
                variacao(faturamento, faturamentoAnterior),
                variacao(BigDecimal.valueOf(validos.size()), BigDecimal.valueOf(pedidosAnteriores.size())));
        QualidadeDados qualidade = new QualidadeDados(
                pedidoService.contarPedidosSemData(),
                pedidos.stream().filter(pedido -> INDEFINIDO.equals(statusDe(pedido))).count(),
                pedidos.stream().filter(pedido -> !possuiValor(pedido)).count());

        return resposta(consulta, new ResumoVendas(
                faturamento,
                pedidos.size(),
                validos.size(),
                cancelados,
                ticketMedio(faturamento, validos.size()),
                itensVendidos,
                descontos,
                percentual(BigDecimal.valueOf(cancelados), BigDecimal.valueOf(pedidos.size())),
                comparativo,
                qualidade));
    }

    public RespostaDashboard<SerieVendas> vendas(FiltroDashboard filtro, String agrupamento) {
        Consulta consulta = resolver(filtro);
        Agrupamento tipo = Agrupamento.de(agrupamento);

        Map<LocalDate, Acumulado> serie = new LinkedHashMap<>();
        for (LocalDate data = tipo.inicioDe(consulta.inicio()); !data.isAfter(consulta.fim()); data = tipo.proximo(data)) {
            serie.put(data, new Acumulado());
        }
        for (Pedido pedido : validos(consulta)) {
            LocalDate data = LocalDate.ofInstant(pedido.getRecebidoEm(), FUSO);
            serie.get(tipo.inicioDe(data)).adicionar(1, valorPedido(pedido, consulta));
        }

        List<VendaPeriodo> pontos = serie.entrySet().stream()
                .map(entrada -> new VendaPeriodo(
                        entrada.getKey(),
                        tipo.rotulo(entrada.getKey()),
                        entrada.getValue().valor,
                        entrada.getValue().quantidade,
                        ticketMedio(entrada.getValue().valor, entrada.getValue().quantidade)))
                .toList();
        return resposta(consulta, new SerieVendas(tipo.name(), pontos));
    }

    public RespostaDashboard<List<VendaHorario>> vendasPorHorario(FiltroDashboard filtro) {
        Consulta consulta = resolver(filtro);
        List<Acumulado> horas = new ArrayList<>();
        for (int hora = 0; hora < 24; hora++) {
            horas.add(new Acumulado());
        }
        for (Pedido pedido : validos(consulta)) {
            int hora = pedido.getRecebidoEm().atZone(FUSO).getHour();
            horas.get(hora).adicionar(1, valorPedido(pedido, consulta));
        }

        List<VendaHorario> resultado = new ArrayList<>();
        for (int hora = 0; hora < 24; hora++) {
            Acumulado acumulado = horas.get(hora);
            resultado.add(new VendaHorario(hora, String.format(Locale.ROOT, "%02dh", hora),
                    acumulado.quantidade, acumulado.valor));
        }
        return resposta(consulta, resultado);
    }

    public RespostaDashboard<List<VendaTipo>> vendasPorTipo(FiltroDashboard filtro) {
        Consulta consulta = resolver(filtro);
        Map<String, Acumulado> tipos = new LinkedHashMap<>();
        for (Pedido pedido : validos(consulta)) {
            for (ItemPedido item : itensConsiderados(pedido, consulta)) {
                tipos.computeIfAbsent(textoOuPadrao(item.getTipoProduto()), chave -> new Acumulado())
                        .adicionar(item.getQuantidade(), valorItem(item, pedido));
            }
        }

        BigDecimal total = tipos.values().stream().map(acumulado -> acumulado.valor).reduce(ZERO, BigDecimal::add);
        List<VendaTipo> resultado = tipos.entrySet().stream()
                .map(entrada -> new VendaTipo(
                        entrada.getKey(),
                        entrada.getValue().quantidade,
                        entrada.getValue().valor,
                        percentual(entrada.getValue().valor, total)))
                .sorted(Comparator.comparing(VendaTipo::faturamento).reversed())
                .toList();
        return resposta(consulta, resultado);
    }

    public RespostaDashboard<SituacaoPedidos> situacaoPedidos(FiltroDashboard filtro) {
        Consulta consulta = resolver(filtro);
        List<Pedido> pedidos = carregar(consulta);
        Map<String, Long> contagem = pedidos.stream()
                .collect(Collectors.groupingBy(this::statusDe, Collectors.counting()));

        List<StatusQuantidade> status = new ArrayList<>();
        BigDecimal total = BigDecimal.valueOf(pedidos.size());
        for (OpcaoFiltro opcao : STATUS) {
            long quantidade = contagem.getOrDefault(opcao.valor(), 0L);
            status.add(new StatusQuantidade(opcao.valor(), opcao.rotulo(), quantidade,
                    percentual(BigDecimal.valueOf(quantidade), total)));
        }
        long indefinidos = contagem.getOrDefault(INDEFINIDO, 0L);
        if (indefinidos > 0) {
            status.add(new StatusQuantidade(INDEFINIDO, "Indefinido", indefinidos,
                    percentual(BigDecimal.valueOf(indefinidos), total)));
        }

        long emAndamento = pedidos.stream().filter(pedido -> STATUS_EM_ANDAMENTO.contains(statusDe(pedido))).count();
        return resposta(consulta, new SituacaoPedidos(pedidos.size(), emAndamento, tempoMedioAtendimento(pedidos), status));
    }

    public RespostaDashboard<List<ProdutoRanking>> produtosMaisVendidos(FiltroDashboard filtro, int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO_RANKING) {
            throw new IllegalArgumentException("O parâmetro 'limite' deve estar entre 1 e " + LIMITE_MAXIMO_RANKING + ".");
        }
        Consulta consulta = resolver(filtro);
        Map<String, AcumuladoProduto> produtos = new LinkedHashMap<>();
        for (Pedido pedido : validos(consulta)) {
            for (ItemPedido item : itensConsiderados(pedido, consulta)) {
                String chave = item.getProdutoId() != null ? item.getProdutoId() : textoOuPadrao(item.getProdutoNome());
                produtos.computeIfAbsent(chave, id -> new AcumuladoProduto(item))
                        .adicionar(item.getQuantidade(), valorItem(item, pedido));
            }
        }

        long quantidadeTotal = produtos.values().stream().mapToLong(produto -> produto.quantidade).sum();
        List<AcumuladoProduto> ordenados = produtos.values().stream()
                .sorted(Comparator.comparingLong((AcumuladoProduto produto) -> produto.quantidade).reversed()
                        .thenComparing(produto -> produto.valor, Comparator.reverseOrder()))
                .limit(limite)
                .toList();

        List<ProdutoRanking> ranking = new ArrayList<>();
        for (int i = 0; i < ordenados.size(); i++) {
            AcumuladoProduto produto = ordenados.get(i);
            ranking.add(new ProdutoRanking(i + 1, produto.produtoId, produto.produtoNome, produto.tipoProduto,
                    produto.quantidade, produto.valor,
                    percentual(BigDecimal.valueOf(produto.quantidade), BigDecimal.valueOf(quantidadeTotal))));
        }
        return resposta(consulta, ranking);
    }

    public RespostaDashboard<ListaPedidos> listarPedidos(FiltroDashboard filtro, int pagina, int tamanho) {
        if (pagina < 0) {
            throw new IllegalArgumentException("O parâmetro 'pagina' não pode ser negativo.");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException("O parâmetro 'tamanho' deve estar entre 1 e " + TAMANHO_MAXIMO_PAGINA + ".");
        }
        Consulta consulta = resolver(filtro);
        List<Pedido> pedidos = carregar(consulta);
        int totalPaginas = (int) Math.ceil((double) pedidos.size() / tamanho);
        List<PedidoResumo> itens = pedidos.stream()
                .skip((long) pagina * tamanho)
                .limit(tamanho)
                .map(pedido -> resumirPedido(pedido, consulta))
                .toList();
        return resposta(consulta, new ListaPedidos(pagina, tamanho, pedidos.size(), totalPaginas, itens));
    }

    public OpcoesFiltro opcoesFiltro() {
        List<Produto> produtos = produtoService.listarProdutos().stream()
                .filter(produto -> produto.getNome() != null)
                .sorted(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<String> tipos = produtos.stream()
                .map(Produto::getTipo)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tipo -> !tipo.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<ProdutoFiltro> opcoesProduto = produtos.stream()
                .map(produto -> new ProdutoFiltro(produto.getId(), produto.getNome(), produto.getTipo()))
                .toList();
        List<OpcaoFiltro> agrupamentos = Arrays.stream(Agrupamento.values())
                .map(agrupamento -> new OpcaoFiltro(agrupamento.name(), agrupamento.descricao))
                .toList();
        LocalDate hoje = LocalDate.now(FUSO);
        return new OpcoesFiltro(STATUS, tipos, opcoesProduto, agrupamentos,
                new PeriodoConsulta(hoje.minusDays(PERIODO_PADRAO_DIAS - 1L), hoje));
    }

    private Consulta resolver(FiltroDashboard filtro) {
        LocalDate hoje = LocalDate.now(FUSO);
        LocalDate fim = filtro.dataFim() != null ? filtro.dataFim() : hoje;
        LocalDate inicio = filtro.dataInicio() != null ? filtro.dataInicio() : fim.minusDays(PERIODO_PADRAO_DIAS - 1L);
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > PERIODO_MAXIMO_DIAS) {
            throw new IllegalArgumentException("O período consultado não pode ultrapassar " + PERIODO_MAXIMO_DIAS + " dias.");
        }
        return new Consulta(inicio, fim, resolverStatus(filtro.status()), textoOuNulo(filtro.tipoProduto()),
                textoOuNulo(filtro.produtoId()));
    }

    private String resolverStatus(String status) {
        if (textoOuNulo(status) == null) {
            return null;
        }
        String normalizado = pedidoService.normalizarStatus(status);
        if (!ROTULOS_STATUS.containsKey(normalizado)) {
            throw new IllegalArgumentException("Status inválido: '" + status + "'. Valores aceitos: "
                    + STATUS.stream().map(OpcaoFiltro::valor).collect(Collectors.joining(", ")) + ".");
        }
        return normalizado;
    }

    private List<Pedido> carregar(Consulta consulta) {
        Instant inicio = consulta.inicio().atStartOfDay(FUSO).toInstant();
        Instant fimExclusivo = consulta.fim().plusDays(1).atStartOfDay(FUSO).toInstant();
        return pedidoService.listarPedidosPorPeriodo(inicio, fimExclusivo).stream()
                .filter(pedido -> consulta.status() == null || consulta.status().equals(statusDe(pedido)))
                .filter(pedido -> !consulta.filtraItens() || pedido.getItens().stream().anyMatch(consulta::aceitaItem))
                .toList();
    }

    private List<Pedido> validos(Consulta consulta) {
        return carregar(consulta).stream().filter(this::valido).toList();
    }

    private boolean valido(Pedido pedido) {
        String status = statusDe(pedido);
        return !CANCELADO.equals(status) && !INDEFINIDO.equals(status);
    }

    private String statusDe(Pedido pedido) {
        return pedido.getStatus() != null && ROTULOS_STATUS.containsKey(pedido.getStatus())
                ? pedido.getStatus() : INDEFINIDO;
    }

    private List<ItemPedido> itensConsiderados(Pedido pedido, Consulta consulta) {
        return pedido.getItens().stream().filter(consulta::aceitaItem).toList();
    }

    private BigDecimal somarValores(List<Pedido> pedidos, Consulta consulta) {
        return pedidos.stream().map(pedido -> valorPedido(pedido, consulta)).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal valorPedido(Pedido pedido, Consulta consulta) {
        if (!consulta.filtraItens() && pedido.getValorTotal() != null) {
            return pedido.getValorTotal();
        }
        return aplicarDesconto(subtotalConsiderado(pedido, consulta), pedido);
    }

    private BigDecimal descontoPedido(Pedido pedido, Consulta consulta) {
        return subtotalConsiderado(pedido, consulta).subtract(valorPedido(pedido, consulta)).max(ZERO);
    }

    private BigDecimal subtotalConsiderado(Pedido pedido, Consulta consulta) {
        return itensConsiderados(pedido, consulta).stream().map(this::subtotalItem).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal valorItem(ItemPedido item, Pedido pedido) {
        return aplicarDesconto(subtotalItem(item), pedido);
    }

    private BigDecimal subtotalItem(ItemPedido item) {
        if (item.getSubtotal() != null) {
            return item.getSubtotal();
        }
        if (item.getPrecoUnitario() == null || item.getQuantidade() < 1) {
            return ZERO;
        }
        return item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal aplicarDesconto(BigDecimal valor, Pedido pedido) {
        BigDecimal percentual = pedido.getPercentualDesconto() == null ? BigDecimal.ZERO : pedido.getPercentualDesconto();
        return valor.multiply(CEM.subtract(percentual)).divide(CEM, 2, RoundingMode.HALF_UP);
    }

    private boolean possuiValor(Pedido pedido) {
        return pedido.getValorTotal() != null
                || pedido.getItens().stream().anyMatch(item -> subtotalItem(item).signum() > 0);
    }

    private BigDecimal ticketMedio(BigDecimal faturamento, long pedidos) {
        return pedidos == 0 ? ZERO : faturamento.divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        return total.signum() == 0 ? ZERO : parte.multiply(CEM).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        return anterior.signum() == 0 ? null
                : atual.subtract(anterior).multiply(CEM).divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private Long tempoMedioAtendimento(List<Pedido> pedidos) {
        List<Pedido> finalizados = pedidos.stream()
                .filter(pedido -> pedido.getFinalizadoEm() != null && pedido.getRecebidoEm() != null)
                .toList();
        if (finalizados.isEmpty()) {
            return null;
        }
        double media = finalizados.stream()
                .mapToLong(pedido -> Duration.between(pedido.getRecebidoEm(), pedido.getFinalizadoEm()).toMinutes())
                .average()
                .orElse(0);
        return Math.round(media);
    }

    private PedidoResumo resumirPedido(Pedido pedido, Consulta consulta) {
        List<ItemPedido> itens = itensConsiderados(pedido, consulta);
        String produtos = itens.stream()
                .map(item -> item.getQuantidade() + "x " + textoOuPadrao(item.getProdutoNome()))
                .collect(Collectors.joining(", "));
        String status = statusDe(pedido);
        return new PedidoResumo(
                pedido.getId(),
                pedido.getRecebidoEm(),
                status,
                ROTULOS_STATUS.getOrDefault(status, "Indefinido"),
                produtos,
                itens.stream().mapToInt(ItemPedido::getQuantidade).sum(),
                pedido.getPercentualDesconto() == null ? BigDecimal.ZERO : pedido.getPercentualDesconto(),
                valorPedido(pedido, consulta));
    }

    private <T> RespostaDashboard<T> resposta(Consulta consulta, T dados) {
        return new RespostaDashboard<>(consulta.periodo(), Instant.now(), dados);
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String textoOuPadrao(String texto) {
        String valor = textoOuNulo(texto);
        return valor == null ? NAO_INFORMADO : valor;
    }

    private record Consulta(LocalDate inicio, LocalDate fim, String status, String tipoProduto, String produtoId) {

        boolean filtraItens() {
            return tipoProduto != null || produtoId != null;
        }

        boolean aceitaItem(ItemPedido item) {
            boolean tipoAceito = tipoProduto == null || tipoProduto.equalsIgnoreCase(textoOuNulo(item.getTipoProduto()));
            boolean produtoAceito = produtoId == null || produtoId.equals(item.getProdutoId());
            return tipoAceito && produtoAceito;
        }

        Consulta periodoAnterior() {
            long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
            return new Consulta(inicio.minusDays(dias), inicio.minusDays(1), status, tipoProduto, produtoId);
        }

        PeriodoConsulta periodo() {
            return new PeriodoConsulta(inicio, fim);
        }
    }

    private enum Agrupamento {
        DIA("Por dia", "dd/MM"),
        SEMANA("Por semana", "dd/MM"),
        MES("Por mês", "MM/yyyy");

        private final String descricao;
        private final DateTimeFormatter formato;

        Agrupamento(String descricao, String padraoRotulo) {
            this.descricao = descricao;
            this.formato = DateTimeFormatter.ofPattern(padraoRotulo);
        }

        static Agrupamento de(String valor) {
            String normalizado = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
            return Arrays.stream(values())
                    .filter(agrupamento -> agrupamento.name().equals(normalizado))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Agrupamento inválido: '" + valor + "'. Valores aceitos: DIA, SEMANA, MES."));
        }

        LocalDate inicioDe(LocalDate data) {
            return switch (this) {
                case DIA -> data;
                case SEMANA -> data.with(DayOfWeek.MONDAY);
                case MES -> data.withDayOfMonth(1);
            };
        }

        LocalDate proximo(LocalDate data) {
            return switch (this) {
                case DIA -> data.plusDays(1);
                case SEMANA -> data.plusWeeks(1);
                case MES -> data.plusMonths(1);
            };
        }

        String rotulo(LocalDate data) {
            return data.format(formato);
        }
    }

    private static class Acumulado {
        long quantidade;
        BigDecimal valor = ZERO;

        void adicionar(long quantidadeAdicional, BigDecimal valorAdicional) {
            quantidade += quantidadeAdicional;
            valor = valor.add(valorAdicional);
        }
    }

    private static final class AcumuladoProduto extends Acumulado {
        private final String produtoId;
        private final String produtoNome;
        private final String tipoProduto;

        private AcumuladoProduto(ItemPedido item) {
            this.produtoId = item.getProdutoId();
            this.produtoNome = textoOuPadrao(item.getProdutoNome());
            this.tipoProduto = textoOuPadrao(item.getTipoProduto());
        }
    }
}
