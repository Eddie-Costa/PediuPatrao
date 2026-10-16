package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.HistoricoStatusPedido;
import com.umc.pediupatrao.entity.HistoricoDescontoPedido;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PedidoService {

    private static final int TAMANHO_MAXIMO_JUSTIFICATIVA_CANCELAMENTO = 1000;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido criarPedido(Pedido pedido, String responsavel, String perfil) {
        // Uma criação sempre gera um novo registro, sem sobrescrever históricos por ID enviado.
        List<ItemPedido> itens = normalizarItens(pedido.getItens(), List.of());
        pedido.setId(null);
        pedido.setStatus("RECEBIDO");
        pedido.setJustificativaCancelamento(null);
        pedido.setRecebidoEm(null);
        pedido.setRecebidoPor(null);
        pedido.setSaidaEm(null);
        pedido.setSaidaPor(null);
        pedido.setFinalizadoEm(null);
        pedido.setFinalizadoPor(null);
        pedido.setCanceladoEm(null);
        pedido.setCanceladoPor(null);
        pedido.setHistoricoStatus(null);
        pedido.setHistoricoDescontos(null);
        pedido.setItens(itens);
        pedido.setPercentualDesconto(BigDecimal.ZERO);
        recalcularValores(pedido);

        String responsavelValidado = validarResponsavel(responsavel);
        Instant agora = Instant.now();
        pedido.setRecebidoEm(agora);
        pedido.setRecebidoPor(responsavelValidado);
        registrarHistorico(pedido, null, "RECEBIDO", agora, responsavelValidado, null);
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PEDIDO", salvo.getId(), "INCLUSAO",
                Map.of(), snapshotPedido(salvo), responsavelValidado, perfil);
        return salvo;
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Optional<Pedido> buscarPorId(String id) {
        return pedidoRepository.findById(id).map(pedido -> {
            garantirItensLegados(pedido);
            return pedido;
        });
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public List<Pedido> listarPedidos() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        pedidos.forEach(this::garantirItensLegados);
        return pedidos;
    }

    // A edição altera apenas os dados do pedido; status segue o fluxo validado abaixo.
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido atualizarPedido(String id, Pedido dadosAtualizados, String responsavel, String perfil) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        String statusAtual = normalizarStatus(pedido.getStatus());
        if (statusAtual.equals("CANCELADO") || statusAtual.equals("FINALIZADO")) {
            throw new IllegalStateException("Pedido cancelado ou finalizado não pode ser alterado.");
        }
        garantirItensLegados(pedido);
        Map<String, Object> valoresAnteriores = snapshotPedido(pedido);
        List<ItemPedido> itensAtualizados = normalizarItens(dadosAtualizados.getItens(), pedido.getItens());
        pedido.setClienteId(dadosAtualizados.getClienteId());
        pedido.setItens(itensAtualizados);
        recalcularValores(pedido);
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PEDIDO", salvo.getId(), "ALTERACAO",
                valoresAnteriores, snapshotPedido(salvo), validarResponsavel(responsavel), perfil);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual, String responsavel) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        garantirItensLegados(pedido);
        Map<String, Object> valoresAnteriores = snapshotPedido(pedido);
        String statusAtual = normalizarStatus(pedido.getStatus());
        if (!Set.of("RECEBIDO", "EM_PREPARACAO", "PRONTO").contains(statusAtual)) {
            throw new IllegalStateException("O desconto só pode ser alterado antes da saída para entrega ou retirada.");
        }
        if (pedido.getItens().isEmpty()) {
            throw new IllegalStateException("O pedido não possui itens registrados para calcular o desconto.");
        }
        if (percentual == null || percentual.compareTo(BigDecimal.ZERO) < 0
                || percentual.compareTo(BigDecimal.valueOf(20)) > 0) {
            throw new IllegalArgumentException("O desconto deve estar entre 0% e 20%.");
        }

        BigDecimal percentualAnterior = pedido.getPercentualDesconto() == null
                ? BigDecimal.ZERO : pedido.getPercentualDesconto();
        BigDecimal valorAnterior = pedido.getValorDesconto() == null
                ? BigDecimal.ZERO : pedido.getValorDesconto();
        BigDecimal totalAnterior = pedido.getValorTotal();
        if (totalAnterior == null) {
            totalAnterior = calcularSubtotal(pedido.getItens()).subtract(valorAnterior);
        }
        pedido.setPercentualDesconto(percentual);
        recalcularValores(pedido);
        pedido.getHistoricoDescontos().add(new HistoricoDescontoPedido(
                percentualAnterior,
                valorAnterior,
                totalAnterior,
                percentual,
                pedido.getValorDesconto(),
                pedido.getValorTotal(),
                validarResponsavel(responsavel),
                "GERENTE",
                Instant.now()));
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PEDIDO", salvo.getId(), "ALTERACAO_DESCONTO",
                valoresAnteriores, snapshotPedido(salvo), validarResponsavel(responsavel), "GERENTE");
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelarPedido(String id, String justificativa, String responsavel) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        garantirItensLegados(pedido);
        Map<String, Object> valoresAnteriores = snapshotPedido(pedido);
        String statusAtual = normalizarStatus(pedido.getStatus());
        if (statusAtual.equals("CANCELADO")) {
            throw new IllegalStateException("Pedido já está cancelado.");
        }
        if (!Set.of("RECEBIDO", "EM_PREPARACAO", "PRONTO").contains(statusAtual)) {
            throw new IllegalStateException("O pedido não pode ser cancelado após sair para entrega/retirada ou ser finalizado.");
        }

        String justificativaNormalizada = justificativa == null ? "" : justificativa.trim();
        if (justificativaNormalizada.isEmpty()) {
            throw new IllegalArgumentException("Informe a justificativa do cancelamento.");
        }
        if (justificativaNormalizada.length() > TAMANHO_MAXIMO_JUSTIFICATIVA_CANCELAMENTO) {
            throw new IllegalArgumentException("A justificativa deve ter no máximo 1000 caracteres.");
        }

        String responsavelValidado = validarResponsavel(responsavel);
        Instant agora = Instant.now();
        pedido.setJustificativaCancelamento(justificativaNormalizada);
        pedido.setStatus("CANCELADO");
        pedido.setCanceladoEm(agora);
        pedido.setCanceladoPor(responsavelValidado);
        registrarHistorico(pedido, statusAtual, "CANCELADO", agora,
                responsavelValidado, justificativaNormalizada);
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PEDIDO", salvo.getId(), "CANCELAMENTO",
                valoresAnteriores, snapshotPedido(salvo), responsavelValidado, "GERENTE");
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido transicionarStatus(String id, String novoStatus, String responsavel) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        garantirItensLegados(pedido);
        Map<String, Object> valoresAnteriores = snapshotPedido(pedido);
        String statusAtual = normalizarStatus(pedido.getStatus());
        String statusDestino = normalizarStatus(novoStatus);

        if (statusAtual.equals("CANCELADO")) {
            throw new IllegalStateException("Pedido cancelado não pode ser alterado.");
        }
        if (statusDestino.equals("CANCELADO")) {
            throw new IllegalArgumentException("O cancelamento exige justificativa e deve ser feito pela ação Cancelar.");
        }

        Set<String> proximosStatus = switch (statusAtual) {
            case "RECEBIDO" -> Set.of("EM_PREPARACAO");
            case "EM_PREPARACAO" -> Set.of("PRONTO");
            case "PRONTO" -> Set.of("SAIU_PARA_ENTREGA", "RETIRADO");
            case "SAIU_PARA_ENTREGA", "RETIRADO" -> Set.of("FINALIZADO");
            default -> Set.of();
        };
        if (!proximosStatus.contains(statusDestino)) {
            throw new IllegalStateException("Transição inválida: " + statusAtual + " → " + statusDestino + ".");
        }

        String responsavelValidado = validarResponsavel(responsavel);
        Instant agora = Instant.now();
        pedido.setStatus(statusDestino);
        if (statusDestino.equals("SAIU_PARA_ENTREGA") || statusDestino.equals("RETIRADO")) {
            pedido.setSaidaEm(agora);
            pedido.setSaidaPor(responsavelValidado);
        } else if (statusDestino.equals("FINALIZADO")) {
            pedido.setFinalizadoEm(agora);
            pedido.setFinalizadoPor(responsavelValidado);
        }
        registrarHistorico(pedido, statusAtual, statusDestino, agora, responsavelValidado, null);
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PEDIDO", salvo.getId(), "ALTERACAO_STATUS",
                valoresAnteriores, snapshotPedido(salvo), responsavelValidado, "GERENTE");
        return salvo;
    }

    private void registrarHistorico(Pedido pedido, String statusAnterior, String statusNovo,
            Instant dataHora, String responsavel, String justificativa) {
        pedido.getHistoricoStatus().add(new HistoricoStatusPedido(
                statusAnterior, statusNovo, dataHora, responsavel, justificativa));
    }

    private String validarResponsavel(String responsavel) {
        if (responsavel == null || responsavel.isBlank()) {
            throw new IllegalArgumentException("Não foi possível identificar o usuário responsável.");
        }
        return responsavel.trim();
    }

    private List<ItemPedido> normalizarItens(List<ItemPedido> itensSolicitados, List<ItemPedido> itensExistentes) {
        if (itensSolicitados == null || itensSolicitados.isEmpty()) {
            throw new IllegalArgumentException("Adicione ao menos um item ao pedido.");
        }

        List<ItemPedido> itensNormalizados = new java.util.ArrayList<>();
        for (ItemPedido itemSolicitado : itensSolicitados) {
            if (itemSolicitado == null) {
                throw new IllegalArgumentException("Um dos itens do pedido é inválido.");
            }
            validarQuantidade(itemSolicitado.getQuantidade());
            if (itemSolicitado.getProdutoId() == null || itemSolicitado.getProdutoId().isBlank()) {
                throw new IllegalArgumentException("Selecione um produto para cada item do pedido.");
            }

            ItemPedido itemExistente = itensExistentes.stream()
                    .filter(item -> itemSolicitado.getProdutoId().equals(item.getProdutoId()))
                    .findFirst()
                    .orElse(null);
            ItemPedido item = new ItemPedido();
            if (itemExistente != null && itemExistente.getPrecoUnitario() != null) {
                item.setProdutoId(itemExistente.getProdutoId());
                item.setProdutoNome(itemExistente.getProdutoNome());
                item.setTipoProduto(itemExistente.getTipoProduto());
                item.setPrecoUnitario(itemExistente.getPrecoUnitario());
            } else {
                Produto produto = buscarProduto(itemSolicitado.getProdutoId());
                item.setProdutoId(produto.getId());
                item.setProdutoNome(produto.getNome());
                item.setTipoProduto(produto.getTipo());
                item.setPrecoUnitario(BigDecimal.valueOf(produto.getPreco()));
            }
            item.setQuantidade(itemSolicitado.getQuantidade());
            item.setSubtotal(calcularSubtotal(item.getPrecoUnitario(), item.getQuantidade()));
            itensNormalizados.add(item);
        }
        return itensNormalizados;
    }

    private Produto buscarProduto(String produtoId) {
        if (produtoId == null || produtoId.isBlank()) {
            throw new IllegalArgumentException("Selecione um produto para o pedido.");
        }
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
        if (produto.getPreco() == null || !Double.isFinite(produto.getPreco()) || produto.getPreco() < 0) {
            throw new IllegalArgumentException("O produto selecionado não possui um preço válido.");
        }
        if (Boolean.FALSE.equals(produto.getAtivo())) {
            throw new IllegalArgumentException("O produto selecionado está inativo.");
        }
        return produto;
    }

    private void garantirItensLegados(Pedido pedido) {
        if (!pedido.getItens().isEmpty() || pedido.getProdutoId() == null) {
            return;
        }

        ItemPedido item = new ItemPedido();
        item.setProdutoId(pedido.getProdutoId());
        item.setProdutoNome(pedido.getProdutoNome());
        item.setTipoProduto(pedido.getTipoPizza());
        item.setQuantidade(pedido.getQuantidade());
        item.setPrecoUnitario(pedido.getPrecoUnitario());
        if (item.getPrecoUnitario() != null && item.getQuantidade() > 0) {
            item.setSubtotal(calcularSubtotal(item.getPrecoUnitario(), item.getQuantidade()));
        }
        pedido.setItens(List.of(item));
    }

    private void validarQuantidade(int quantidade) {
        if (quantidade < 1) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        return itens.stream()
                .map(item -> calcularSubtotal(item.getPrecoUnitario(), item.getQuantidade()))
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
    }

    private BigDecimal calcularSubtotal(BigDecimal precoUnitario, int quantidade) {
        if (precoUnitario == null) {
            throw new IllegalStateException("Um item do pedido não possui preço registrado.");
        }
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> snapshotPedido(Pedido pedido) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("clienteId", pedido.getClienteId());
        valores.put("itens", pedido.getItens().stream().map(item -> {
            Map<String, Object> dadosItem = new LinkedHashMap<>();
            dadosItem.put("produtoId", item.getProdutoId());
            dadosItem.put("produtoNome", item.getProdutoNome());
            dadosItem.put("tipoProduto", item.getTipoProduto());
            dadosItem.put("quantidade", item.getQuantidade());
            dadosItem.put("precoUnitario", item.getPrecoUnitario());
            dadosItem.put("subtotal", item.getSubtotal());
            return dadosItem;
        }).toList());
        valores.put("percentualDesconto", pedido.getPercentualDesconto());
        valores.put("valorDesconto", pedido.getValorDesconto());
        valores.put("valorTotal", pedido.getValorTotal());
        valores.put("status", pedido.getStatus());
        valores.put("recebidoEm", pedido.getRecebidoEm());
        valores.put("recebidoPor", pedido.getRecebidoPor());
        valores.put("saidaEm", pedido.getSaidaEm());
        valores.put("saidaPor", pedido.getSaidaPor());
        valores.put("finalizadoEm", pedido.getFinalizadoEm());
        valores.put("finalizadoPor", pedido.getFinalizadoPor());
        valores.put("canceladoEm", pedido.getCanceladoEm());
        valores.put("canceladoPor", pedido.getCanceladoPor());
        valores.put("justificativaCancelamento", pedido.getJustificativaCancelamento());
        return valores;
    }

    private void recalcularValores(Pedido pedido) {
        BigDecimal subtotal = calcularSubtotal(pedido.getItens());
        BigDecimal percentual = pedido.getPercentualDesconto() == null
                ? BigDecimal.ZERO : pedido.getPercentualDesconto();
        if (percentual.compareTo(BigDecimal.ZERO) < 0 || percentual.compareTo(BigDecimal.valueOf(20)) > 0) {
            throw new IllegalArgumentException("O desconto deve estar entre 0% e 20%.");
        }
        BigDecimal valorDesconto = subtotal.multiply(percentual)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        pedido.setValorDesconto(valorDesconto);
        pedido.setValorTotal(subtotal.subtract(valorDesconto).setScale(2, RoundingMode.HALF_UP));

        if (!pedido.getItens().isEmpty()) {
            ItemPedido primeiroItem = pedido.getItens().get(0);
            pedido.setProdutoId(primeiroItem.getProdutoId());
            pedido.setProdutoNome(primeiroItem.getProdutoNome());
            pedido.setTipoPizza(primeiroItem.getTipoProduto());
            pedido.setSabor(primeiroItem.getProdutoNome());
            pedido.setPrecoUnitario(primeiroItem.getPrecoUnitario());
            pedido.setQuantidade(pedido.getItens().stream()
                    .mapToInt(ItemPedido::getQuantidade)
                    .sum());
        }
    }

    private String normalizarStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("O status do pedido é obrigatório.");
        }
        String normalizado = status.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (normalizado) {
            case "PENDENTE" -> "RECEBIDO";
            case "EM_PREPARO" -> "EM_PREPARACAO";
            case "ENTREGUE" -> "FINALIZADO";
            default -> normalizado;
        };
    }

}
