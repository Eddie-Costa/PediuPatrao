package com.umc.pediupatrao.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.GrantedAuthority;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuditoriaService auditoriaService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("content", "home :: content");
        return "layout";
    }

    // ========================
    // USUÁRIOS
    // ========================
    @GetMapping("/usuarios")
    public String listarUsuarios(Model model, Authentication authentication) {
        List<Usuario> usuarios = (List<Usuario>) usuarioService.listarTodos();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("loggedInUsername", authentication.getName());
        model.addAttribute("content", "usuarios/lista :: content");
        log.info("Carregando fragmento: usuarios/lista :: content");
        return "layout";
    }

    @GetMapping("/usuarios/novo")
    public String novoUsuarioForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuarioForm(@PathVariable String id, Model model, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        boolean encontrado = usuarioService.buscarPorId(id).map(u -> {
            model.addAttribute("usuario", u);
            return true;
        }).orElse(false);

        if (!encontrado) {
            redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        }

        model.addAttribute("loggedInUsername", authentication.getName());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @PostMapping("/usuarios/salvar")
    public String salvarUsuario(@ModelAttribute Usuario usuario,
            RedirectAttributes redirectAttributes) {
        usuarioService.salvarUsuario(usuario);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário criado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/editar/{id}")
    public String atualizarUsuario(@PathVariable String id,
            @ModelAttribute Usuario usuario,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        usuarioService.atualizarUsuario(id, usuario, authentication.getName());
        redirectAttributes.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/deletar/{id}")
    public String deletarUsuario(@PathVariable String id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        usuarioService.deletarUsuario(id, authentication.getName());
        redirectAttributes.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        return "redirect:/usuarios";
    }

    // ========================
    // PEDIDOS
    // ========================
    @GetMapping("/pedidos")
    public String pedidos(Model model, Authentication authentication) {
        List<Pedido> pedidos = pedidoService.listarPedidos();
        model.addAttribute("pedidos", pedidos);
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean gerente = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_GERENTE".equals(authority.getAuthority()));
        boolean atendente = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ATENDENTE".equals(authority.getAuthority()));
        boolean podeCriarPedido = gerente || atendente;
        model.addAttribute("canManageOrders", gerente);
        model.addAttribute("canCancelOrders", gerente);
        model.addAttribute("canTransitionOrders", gerente);
        model.addAttribute("canCreateOrders", podeCriarPedido);
        model.addAttribute("canViewDiscountHistory", admin || gerente);
        model.addAttribute("content", "pedidos :: content");
        log.info("Carregando fragmento: pedidos :: content");
        return "layout";
    }

    @GetMapping("/auditoria")
    public String auditoria(Model model) {
        model.addAttribute("registros", auditoriaService.listarTodos());
        model.addAttribute("content", "auditoria/lista :: content");
        return "layout";
    }

    @GetMapping("/pedidos/novo")
    public String novoPedidoForm(Model model) {
        Pedido pedido = new Pedido();
        ItemPedido item = new ItemPedido();
        item.setQuantidade(1);
        pedido.setItens(List.of(item));
        model.addAttribute("pedido", pedido);
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("produtos", produtoService.listarProdutosDisponiveis());
        model.addAttribute("content", "pedidos/form :: content");
        return "layout";
    }

    @PostMapping("/pedidos/salvar")
    public String salvarPedido(@ModelAttribute Pedido pedido, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        pedidoService.criarPedido(pedido, authentication.getName(), perfil(authentication));
        redirectAttributes.addFlashAttribute("sucesso", "Pedido criado com sucesso!");
        return "redirect:/pedidos";
    }

    @GetMapping("/pedidos/editar/{id}")
    public String editarPedidoForm(@PathVariable String id, Model model,
        RedirectAttributes redirectAttributes) {
        var pedidoEncontrado = pedidoService.buscarPorId(id);
        if (pedidoEncontrado.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Pedido nao encontrado.");
            return "redirect:/pedidos";
        }

        Pedido pedido = pedidoEncontrado.get();
        if ("CANCELADO".equalsIgnoreCase(pedido.getStatus())
                || "FINALIZADO".equalsIgnoreCase(pedido.getStatus())
                || "ENTREGUE".equalsIgnoreCase(pedido.getStatus())) {
            redirectAttributes.addFlashAttribute("erro", "Pedido cancelado ou finalizado não pode ser alterado.");
            return "redirect:/pedidos";
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("clientes", clienteService.listarClientes());
        List<Produto> produtosDisponiveis = new ArrayList<>(produtoService.listarProdutosDisponiveis());
        for (ItemPedido item : pedido.getItens()) {
            if (item.getProdutoId() != null
                    && produtosDisponiveis.stream()
                            .noneMatch(produto -> item.getProdutoId().equals(produto.getId()))) {
                produtoService.buscarPorId(item.getProdutoId()).ifPresent(produtosDisponiveis::add);
            }
        }
        model.addAttribute("produtos", produtosDisponiveis);

        model.addAttribute("content", "pedidos/form :: content");
        return "layout";
    }

    @PostMapping("/pedidos/editar/{id}")
    public String atualizarPedido(@PathVariable String id, @ModelAttribute Pedido pedido,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        pedidoService.atualizarPedido(id, pedido, authentication.getName(), perfil(authentication));
        redirectAttributes.addFlashAttribute("sucesso", "Pedido atualizado com sucesso!");
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{id}/cancelar")
    public String cancelarPedido(@PathVariable String id,
            @RequestParam(name = "justificativa", required = false) String justificativa,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            pedidoService.cancelarPedido(id, justificativa, authentication.getName());
            redirectAttributes.addFlashAttribute("sucesso", "Pedido cancelado com sucesso!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{id}/status")
    public String transicionarStatus(@PathVariable String id,
            @RequestParam(name = "status") String status,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            pedidoService.transicionarStatus(id, status, authentication.getName());
            redirectAttributes.addFlashAttribute("sucesso", "Status do pedido atualizado com sucesso!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{id}/desconto")
    public String aplicarDesconto(@PathVariable String id,
            @RequestParam(name = "percentual") BigDecimal percentual,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            pedidoService.aplicarDesconto(id, percentual, authentication.getName());
            redirectAttributes.addFlashAttribute("sucesso", "Desconto aplicado com sucesso!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos";
    }

    // ========================
    // PRODUTOS
    // ========================
    @GetMapping("/produtos")
    public String produtos(Model model, Authentication authentication) {
        List<Produto> produtos = produtoService.listarProdutos();
        model.addAttribute("produtos", produtos);
        boolean podeGerenciarProdutos = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_GERENTE".equals(authority.getAuthority()));
        model.addAttribute("canManageProducts", podeGerenciarProdutos);
        model.addAttribute("content", "produtos/lista :: content");
        log.info("Carregando fragmento: produtos/lista :: content");
        return "layout";
    }

    @GetMapping("/produtos/novo")
    public String novoProdutoForm(Model model) {
        model.addAttribute("produto", new Produto());
        model.addAttribute("content", "produtos/form :: content");
        return "layout";
    }

    @GetMapping("/produtos/editar/{id}")
    public String editarProdutoForm(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        var produtoEncontrado = produtoService.buscarPorId(id);
        if (produtoEncontrado.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Produto não encontrado.");
            return "redirect:/produtos";
        }

        model.addAttribute("produto", produtoEncontrado.get());
        model.addAttribute("content", "produtos/form :: content");
        return "layout";
    }

    @PostMapping("/produtos/salvar")
    public String salvarProduto(@ModelAttribute Produto produto,
            RedirectAttributes redirectAttributes) {
        if (produto.getId() != null && produto.getId().isBlank()) {
            produto.setId(null);
        }
        try {
            produtoService.salvar(produto);
            redirectAttributes.addFlashAttribute("sucesso", "Produto salvo com sucesso.");
            return "redirect:/produtos";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return produto.getId() == null || produto.getId().isBlank()
                    ? "redirect:/produtos/novo"
                    : "redirect:/produtos/editar/" + produto.getId();
        }
    }

    // ========================
    // CLIENTES
    // ========================
    @GetMapping("/clientes")
    public String clientes(Model model, Authentication authentication) {
        List<Cliente> clientes = clienteService.listarClientes();
        model.addAttribute("clientes", clientes);
        model.addAttribute("canEditCustomers", authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ATENDENTE".equals(authority.getAuthority())));
        model.addAttribute("content", "clientes/lista :: content");
        log.info("Carregando fragmento: clientes/lista :: content");
        return "layout";
    }

    @GetMapping("/clientes/novo")
    public String novoClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/salvar")
    public String salvarCliente(@ModelAttribute Cliente cliente,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        clienteService.salvar(cliente, authentication.getName(), perfil(authentication));
        redirectAttributes.addFlashAttribute("sucesso", "Cliente criado com sucesso!");
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    public String editarClienteForm(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        boolean encontrado = clienteService.buscarPorId(id).map(c -> {
            model.addAttribute("cliente", c);
            return true;
        }).orElse(false);

        if (!encontrado) {
            redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes";
        }

        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/editar/{id}")
    public String atualizarCliente(@PathVariable String id,
            @ModelAttribute Cliente cliente,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        cliente.setId(id);
        clienteService.salvar(cliente, authentication.getName(), perfil(authentication));
        redirectAttributes.addFlashAttribute("sucesso", "Cliente atualizado com sucesso!");
        return "redirect:/clientes";
    }

    // ========================
    // OUTRAS ROTAS
    // ========================
    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        model.addAttribute("content", "configuracoes :: content");
        return "layout";
    }

    @GetMapping("/teste")
    public String teste() {
        return "teste";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    private String perfil(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.equals("ROLE_ADMIN")
                        || authority.equals("ROLE_GERENTE")
                        || authority.equals("ROLE_ATENDENTE"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Não foi possível identificar o perfil do usuário autenticado."));
    }
}
