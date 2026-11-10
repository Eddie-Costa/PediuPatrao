package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.HistoricoStatusPedido;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Configuration
@Profile("carga-demo")
public class CargaDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaDemoRunner.class);
    private static final String MARCADOR = "carga-demo";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int DIAS_HISTORICO = 120;
    private static final int PEDIDOS_EM_ANDAMENTO = 15;
    private static final List<BigDecimal> DESCONTOS = List.of(
            BigDecimal.valueOf(5), BigDecimal.valueOf(10), BigDecimal.valueOf(15), BigDecimal.valueOf(20));
    private static final List<String> STATUS_LEGADOS = List.of(
            "ENTREGUE", "ENTREGUE", "ENTREGUE", "ENTREGUE", "ENTREGUE", "ENTREGUE", "PENDENTE", "EM_PREPARO");
    private static final List<String> JUSTIFICATIVAS = List.of(
            "Cliente desistiu do pedido.",
            "Endereco fora da area de entrega.",
            "Ingrediente indisponivel.",
            "Pedido duplicado.");
    private static final List<String> NOMES_CLIENTES = List.of(
            "Ana Souza", "Bruno Lima", "Carla Dias", "Diego Rocha", "Elisa Nunes",
            "Felipe Melo", "Gabriela Reis", "Heitor Prado", "Isabela Cruz", "Joao Tavares",
            "Karina Lopes", "Lucas Pires", "Marina Alves", "Nicolas Farias", "Olivia Ramos");
    private static final List<ProdutoDemo> CATALOGO = List.of(
            new ProdutoDemo("Pizza Calabresa", "Pizza", 49.90, 22),
            new ProdutoDemo("Pizza Mussarela", "Pizza", 44.90, 18),
            new ProdutoDemo("Pizza Portuguesa", "Pizza", 54.90, 12),
            new ProdutoDemo("Pizza Frango com Catupiry", "Pizza", 56.90, 14),
            new ProdutoDemo("Esfiha de Carne", "Esfiha", 7.50, 10),
            new ProdutoDemo("Esfiha de Queijo", "Esfiha", 7.00, 7),
            new ProdutoDemo("X-Burger", "Lanche", 24.90, 9),
            new ProdutoDemo("X-Salada", "Lanche", 27.90, 6),
            new ProdutoDemo("Refrigerante 2L", "Bebida", 14.00, 16),
            new ProdutoDemo("Suco Natural", "Bebida", 11.00, 4));

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public void run(String... args) {
        Query cargaExistente = Query.query(Criteria.where("recebidoPor").is(MARCADOR));
        if (mongoTemplate.exists(cargaExistente, Pedido.class)) {
            log.info("Carga de demonstracao ja aplicada; nenhum dado foi inserido.");
            return;
        }

        long pedidosAntes = pedidoRepository.count();
        Random random = new Random(20261109L);
        Instant agora = Instant.now();
        Map<Produto, Integer> produtos = prepararProdutos();
        List<Cliente> clientes = prepararClientes();

        List<Pedido> pedidos = new ArrayList<>();
        pedidos.addAll(gerarHistorico(produtos, clientes, random, agora));
        pedidos.addAll(gerarEmAndamento(produtos, clientes, random, agora));
        pedidos.addAll(gerarLegados(produtos, clientes, random, agora));
        pedidoRepository.saveAll(pedidos);

        log.info("Carga de demonstracao concluida: {} pedidos inseridos ({} antes, {} depois), {} produtos e {} clientes disponiveis.",
                pedidos.size(), pedidosAntes, pedidoRepository.count(), produtos.size(), clientes.size());
    }

    private Map<Produto, Integer> prepararProdutos() {
        List<Produto> existentes = produtoRepository.findAll();
        Map<Produto, Integer> produtos = new LinkedHashMap<>();
        for (ProdutoDemo demo : CATALOGO) {
            Produto produto = existentes.stream()
                    .filter(existente -> demo.nome().equalsIgnoreCase(existente.getNome()))
                    .filter(existente -> existente.getPreco() != null)
                    .findFirst()
                    .orElseGet(() -> criarProduto(demo));
            produtos.put(produto, demo.peso());
        }
        return produtos;
    }

    private Produto criarProduto(ProdutoDemo demo) {
        Produto produto = new Produto();
        produto.setNome(demo.nome());
        produto.setTipo(demo.tipo());
        produto.setPreco(demo.preco());
        produto.setAtivo(true);
        return produtoRepository.save(produto);
    }

    private List<Cliente> prepararClientes() {
        List<Cliente> clientes = new ArrayList<>();
        for (int i = 0; i < NOMES_CLIENTES.size(); i++) {
            String telefone = String.format(Locale.ROOT, "1190000%04d", i + 1);
            int indice = i;
            Cliente cliente = clienteRepository.findByTelefone(telefone).stream()
                    .findFirst()
                    .orElseGet(() -> criarCliente(NOMES_CLIENTES.get(indice), telefone));
            clientes.add(cliente);
        }
        return clientes;
    }

    private Cliente criarCliente(String nome, String telefone) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setTelefone(telefone);
        cliente.setCidade("Mogi das Cruzes");
        cliente.setEstado("SP");
        return clienteRepository.save(cliente);
    }

    private List<Pedido> gerarHistorico(Map<Produto, Integer> produtos, List<Cliente> clientes,
            Random random, Instant agora) {
        List<Pedido> pedidos = new ArrayList<>();
        LocalDate hoje = LocalDate.ofInstant(agora, FUSO);
        for (int diasAtras = DIAS_HISTORICO; diasAtras >= 0; diasAtras--) {
            LocalDate dia = hoje.minusDays(diasAtras);
            int quantidade = quantidadeDoDia(dia, diasAtras, random);
            for (int i = 0; i < quantidade; i++) {
                Instant recebidoEm = horarioDoPedido(dia, random);
                if (recebidoEm.isAfter(agora)) {
                    continue;
                }
                Pedido pedido = novoPedido(produtos, clientes, random, recebidoEm);
                if (random.nextInt(100) < 9) {
                    cancelar(pedido, random, agora);
                } else {
                    percorrerFluxo(pedido, 4, random, agora);
                }
                pedidos.add(pedido);
            }
        }
        return pedidos;
    }

    private int quantidadeDoDia(LocalDate dia, int diasAtras, Random random) {
        int base = switch (dia.getDayOfWeek()) {
            case MONDAY -> 6;
            case FRIDAY -> 16;
            case SATURDAY -> 20;
            case SUNDAY -> 18;
            default -> 9;
        };
        double crescimento = 0.75 + 0.5 * (DIAS_HISTORICO - diasAtras) / DIAS_HISTORICO;
        return Math.max(1, (int) Math.round(base * crescimento) + random.nextInt(5) - 2);
    }

    private Instant horarioDoPedido(LocalDate dia, Random random) {
        int hora = random.nextInt(100) < 85 ? 18 + random.nextInt(6) : 11 + random.nextInt(4);
        return dia.atTime(hora, random.nextInt(60), random.nextInt(60)).atZone(FUSO).toInstant();
    }

    private List<Pedido> gerarEmAndamento(Map<Produto, Integer> produtos, List<Cliente> clientes,
            Random random, Instant agora) {
        List<Pedido> pedidos = new ArrayList<>();
        for (int i = 0; i < PEDIDOS_EM_ANDAMENTO; i++) {
            Instant recebidoEm = agora.minus(5L + random.nextInt(85), ChronoUnit.MINUTES);
            Pedido pedido = novoPedido(produtos, clientes, random, recebidoEm);
            percorrerFluxo(pedido, i % 4, random, agora);
            pedidos.add(pedido);
        }
        return pedidos;
    }

    private List<Pedido> gerarLegados(Map<Produto, Integer> produtos, List<Cliente> clientes,
            Random random, Instant agora) {
        List<Produto> catalogo = new ArrayList<>(produtos.keySet());
        List<Pedido> pedidos = new ArrayList<>();
        for (int i = 0; i < STATUS_LEGADOS.size(); i++) {
            Produto produto = catalogo.get(random.nextInt(catalogo.size()));
            Pedido pedido = new Pedido();
            pedido.setClienteId(clientes.get(random.nextInt(clientes.size())).getId());
            pedido.setStatus(STATUS_LEGADOS.get(i));
            pedido.setRecebidoEm(agora.minus(60L + random.nextInt(50), ChronoUnit.DAYS));
            pedido.setRecebidoPor(MARCADOR);
            pedido.setProdutoId(produto.getId());
            pedido.setProdutoNome(produto.getNome());
            pedido.setTipoPizza(produto.getTipo());
            pedido.setSabor(produto.getNome());
            pedido.setQuantidade(1 + random.nextInt(3));
            pedido.setPrecoUnitario(BigDecimal.valueOf(produto.getPreco()));
            pedidos.add(pedido);
        }
        return pedidos;
    }

    private Pedido novoPedido(Map<Produto, Integer> produtos, List<Cliente> clientes,
            Random random, Instant recebidoEm) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(clientes.get(random.nextInt(clientes.size())).getId());
        pedido.setItens(gerarItens(produtos, random));
        pedido.setPercentualDesconto(random.nextInt(100) < 15
                ? DESCONTOS.get(random.nextInt(DESCONTOS.size()))
                : BigDecimal.ZERO);
        preencherValores(pedido);
        pedido.setRecebidoEm(recebidoEm);
        pedido.setRecebidoPor(MARCADOR);
        registrarStatus(pedido, "RECEBIDO", recebidoEm, null);
        return pedido;
    }

    private List<ItemPedido> gerarItens(Map<Produto, Integer> produtos, Random random) {
        Map<String, ItemPedido> itens = new LinkedHashMap<>();
        int quantidadeItens = 1 + random.nextInt(4);
        for (int i = 0; i < quantidadeItens; i++) {
            Produto produto = sortearProduto(produtos, random);
            if (itens.containsKey(produto.getId())) {
                continue;
            }
            ItemPedido item = new ItemPedido();
            item.setProdutoId(produto.getId());
            item.setProdutoNome(produto.getNome());
            item.setTipoProduto(produto.getTipo());
            item.setQuantidade(1 + random.nextInt(3));
            item.setPrecoUnitario(BigDecimal.valueOf(produto.getPreco()));
            item.setSubtotal(item.getPrecoUnitario()
                    .multiply(BigDecimal.valueOf(item.getQuantidade()))
                    .setScale(2, RoundingMode.HALF_UP));
            itens.put(produto.getId(), item);
        }
        return new ArrayList<>(itens.values());
    }

    private Produto sortearProduto(Map<Produto, Integer> produtos, Random random) {
        int total = produtos.values().stream().mapToInt(Integer::intValue).sum();
        int sorteio = random.nextInt(total);
        for (Map.Entry<Produto, Integer> entrada : produtos.entrySet()) {
            sorteio -= entrada.getValue();
            if (sorteio < 0) {
                return entrada.getKey();
            }
        }
        throw new IllegalStateException("Catalogo de demonstracao vazio.");
    }

    private void preencherValores(Pedido pedido) {
        BigDecimal subtotal = pedido.getItens().stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        BigDecimal valorDesconto = subtotal.multiply(pedido.getPercentualDesconto())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        pedido.setValorDesconto(valorDesconto);
        pedido.setValorTotal(subtotal.subtract(valorDesconto).setScale(2, RoundingMode.HALF_UP));

        ItemPedido primeiroItem = pedido.getItens().get(0);
        pedido.setProdutoId(primeiroItem.getProdutoId());
        pedido.setProdutoNome(primeiroItem.getProdutoNome());
        pedido.setTipoPizza(primeiroItem.getTipoProduto());
        pedido.setSabor(primeiroItem.getProdutoNome());
        pedido.setPrecoUnitario(primeiroItem.getPrecoUnitario());
        pedido.setQuantidade(pedido.getItens().stream().mapToInt(ItemPedido::getQuantidade).sum());
    }

    private void percorrerFluxo(Pedido pedido, int etapas, Random random, Instant agora) {
        String saida = random.nextInt(100) < 70 ? "SAIU_PARA_ENTREGA" : "RETIRADO";
        List<String> fluxo = List.of("EM_PREPARACAO", "PRONTO", saida, "FINALIZADO");
        Instant momento = pedido.getRecebidoEm();
        for (int i = 0; i < etapas; i++) {
            momento = proximoMomento(momento, random, agora);
            String status = fluxo.get(i);
            registrarStatus(pedido, status, momento, null);
            if (status.equals(saida)) {
                pedido.setSaidaEm(momento);
                pedido.setSaidaPor(MARCADOR);
            } else if (status.equals("FINALIZADO")) {
                pedido.setFinalizadoEm(momento);
                pedido.setFinalizadoPor(MARCADOR);
            }
        }
    }

    private void cancelar(Pedido pedido, Random random, Instant agora) {
        List<String> anteriores = List.of("EM_PREPARACAO", "PRONTO");
        Instant momento = pedido.getRecebidoEm();
        int etapasConcluidas = random.nextInt(anteriores.size() + 1);
        for (int i = 0; i < etapasConcluidas; i++) {
            momento = proximoMomento(momento, random, agora);
            registrarStatus(pedido, anteriores.get(i), momento, null);
        }
        momento = proximoMomento(momento, random, agora);
        String justificativa = JUSTIFICATIVAS.get(random.nextInt(JUSTIFICATIVAS.size()));
        registrarStatus(pedido, "CANCELADO", momento, justificativa);
        pedido.setJustificativaCancelamento(justificativa);
        pedido.setCanceladoEm(momento);
        pedido.setCanceladoPor(MARCADOR);
    }

    private Instant proximoMomento(Instant atual, Random random, Instant agora) {
        Instant proximo = atual.plus(5L + random.nextInt(25), ChronoUnit.MINUTES);
        return proximo.isAfter(agora) ? agora : proximo;
    }

    private void registrarStatus(Pedido pedido, String novoStatus, Instant momento, String justificativa) {
        pedido.getHistoricoStatus().add(new HistoricoStatusPedido(
                pedido.getStatus(), novoStatus, momento, MARCADOR, justificativa));
        pedido.setStatus(novoStatus);
    }

    private record ProdutoDemo(String nome, String tipo, double preco, int peso) {
    }
}
