package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public Pedido criarPedido(@RequestBody Pedido pedido, Authentication authentication) {
        String perfil = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.equals("ROLE_GERENTE")
                        || authority.equals("ROLE_ATENDENTE"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Não foi possível identificar o perfil do usuário autenticado."));
        return pedidoService.criarPedido(pedido, authentication.getName(), perfil);
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam(name = "status") String status,
            Authentication authentication) {
        return pedidoService.transicionarStatus(id, status, authentication.getName());
    }

    @PostMapping("/{id}/desconto")
    public Pedido aplicarDesconto(@PathVariable String id,
            @RequestParam(name = "percentual") BigDecimal percentual,
            Authentication authentication) {
        return pedidoService.aplicarDesconto(id, percentual, authentication.getName());
    }

    @PostMapping("/{id}/cancelamento")
    public Pedido cancelarPedido(@PathVariable String id,
            @RequestParam(name = "justificativa") String justificativa, Authentication authentication) {
        return pedidoService.cancelarPedido(id, justificativa, authentication.getName());
    }
    
}
