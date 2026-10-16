package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.RegistroAuditoriaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private RegistroAuditoriaRepository auditoriaRepository;

    @InjectMocks
    private PedidoService pedidoService;

    @BeforeEach
    void configurarAuditoria() {
        ReflectionTestUtils.setField(pedidoService, "auditoriaService",
                new AuditoriaService(auditoriaRepository));
    }

    @Test
    void criaPedidoComVariosItensECalculaValoresNoServidor() {
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Produto pizza = produto("pizza-1", "Calabresa", "Pizza", 29.90);
        Produto bebida = produto("bebida-1", "Refrigerante", "Bebida", 12.50);
        when(produtoRepository.findById("pizza-1")).thenReturn(Optional.of(pizza));
        when(produtoRepository.findById("bebida-1")).thenReturn(Optional.of(bebida));

        Pedido requisicao = new Pedido();
        requisicao.setItens(List.of(
                item("pizza-1", 2, "0.01"),
                item("bebida-1", 1, "0.01")));
        requisicao.setPercentualDesconto(new BigDecimal("90"));
        requisicao.setValorTotal(new BigDecimal("0.01"));
        requisicao.setRecebidoPor("usuario-forjado");

        Pedido salvo = pedidoService.criarPedido(requisicao, "atendente", "ATENDENTE");

        assertEquals(2, salvo.getItens().size());
        assertEquals(new BigDecimal("59.80"), salvo.getItens().get(0).getSubtotal());
        assertEquals(new BigDecimal("12.50"), salvo.getItens().get(1).getSubtotal());
        assertEquals(new BigDecimal("72.30"), salvo.getValorTotal());
        assertEquals(BigDecimal.ZERO, salvo.getPercentualDesconto());
        assertEquals("atendente", salvo.getRecebidoPor());
        assertNotNull(salvo.getRecebidoEm());
        verify(auditoriaRepository).save(argThat(registro ->
                "PEDIDO".equals(registro.getEntidade())
                        && "INCLUSAO".equals(registro.getOperacao())
                        && "atendente".equals(registro.getUsuario())
                        && "ATENDENTE".equals(registro.getPerfil())
                        && registro.getValoresAnteriores().isEmpty()
                        && registro.getValoresNovos().containsKey("valorTotal")));
    }

    @Test
    void aplicaDescontoSobreSubtotalDeTodosOsItens() {
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Produto pizza = produto("pizza-1", "Calabresa", "Pizza", 29.90);
        Produto bebida = produto("bebida-1", "Refrigerante", "Bebida", 12.50);
        when(produtoRepository.findById("pizza-1")).thenReturn(Optional.of(pizza));
        when(produtoRepository.findById("bebida-1")).thenReturn(Optional.of(bebida));

        Pedido pedido = new Pedido();
        pedido.setItens(List.of(item("pizza-1", 2, "0.01"), item("bebida-1", 1, "0.01")));
        Pedido salvo = pedidoService.criarPedido(pedido, "atendente", "ATENDENTE");
        salvo.setId("pedido-1");
        when(pedidoRepository.findById("pedido-1")).thenReturn(Optional.of(salvo));

        Pedido atualizado = pedidoService.aplicarDesconto(
                "pedido-1", new BigDecimal("10"), "gerente");

        assertEquals(new BigDecimal("7.23"), atualizado.getValorDesconto());
        assertEquals(new BigDecimal("65.07"), atualizado.getValorTotal());
        verify(auditoriaRepository).save(argThat(registro ->
                "ALTERACAO_DESCONTO".equals(registro.getOperacao())
                        && "gerente".equals(registro.getUsuario())
                        && "GERENTE".equals(registro.getPerfil())
                        && registro.getValoresAnteriores().containsKey("valorTotal")
                        && registro.getValoresNovos().containsKey("valorTotal")));
    }

    @Test
    void rejeitaPedidoSemItens() {
        Pedido pedido = new Pedido();

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.criarPedido(pedido, "atendente", "ATENDENTE"));
    }

    @Test
    void registraAuditoriaAoAlterarStatus() {
        Pedido pedido = pedidoExistente("pedido-status");
        when(pedidoRepository.findById("pedido-status")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pedidoService.transicionarStatus("pedido-status", "EM_PREPARACAO", "gerente");

        verify(auditoriaRepository).save(argThat(registro ->
                "ALTERACAO_STATUS".equals(registro.getOperacao())
                        && "RECEBIDO".equals(registro.getValoresAnteriores().get("status"))
                        && "EM_PREPARACAO".equals(registro.getValoresNovos().get("status"))
                        && "gerente".equals(registro.getUsuario())
                        && "GERENTE".equals(registro.getPerfil())));
    }

    @Test
    void registraAuditoriaAoCancelarPedido() {
        Pedido pedido = pedidoExistente("pedido-cancelamento");
        when(pedidoRepository.findById("pedido-cancelamento")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pedidoService.cancelarPedido("pedido-cancelamento", "Cliente desistiu", "gerente");

        verify(auditoriaRepository).save(argThat(registro ->
                "CANCELAMENTO".equals(registro.getOperacao())
                        && "RECEBIDO".equals(registro.getValoresAnteriores().get("status"))
                        && "CANCELADO".equals(registro.getValoresNovos().get("status"))
                        && "Cliente desistiu".equals(registro.getValoresNovos().get("justificativaCancelamento"))));
    }

    private Produto produto(String id, String nome, String tipo, double preco) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setTipo(tipo);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }

    private ItemPedido item(String produtoId, int quantidade, String precoForjado) {
        ItemPedido item = new ItemPedido();
        item.setProdutoId(produtoId);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(new BigDecimal(precoForjado));
        item.setSubtotal(new BigDecimal(precoForjado));
        return item;
    }

    private Pedido pedidoExistente(String id) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setStatus("RECEBIDO");
        pedido.setProdutoId("produto-1");
        pedido.setProdutoNome("Pizza");
        pedido.setQuantidade(1);
        pedido.setPrecoUnitario(new BigDecimal("20.00"));
        pedido.setPercentualDesconto(BigDecimal.ZERO);
        pedido.setValorDesconto(BigDecimal.ZERO);
        pedido.setValorTotal(new BigDecimal("20.00"));
        return pedido;
    }
}
