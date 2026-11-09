package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.DashboardDtos.FiltroDashboard;
import com.umc.pediupatrao.dto.DashboardDtos.ListaPedidos;
import com.umc.pediupatrao.dto.DashboardDtos.OpcoesFiltro;
import com.umc.pediupatrao.dto.DashboardDtos.ProdutoRanking;
import com.umc.pediupatrao.dto.DashboardDtos.RespostaDashboard;
import com.umc.pediupatrao.dto.DashboardDtos.ResumoVendas;
import com.umc.pediupatrao.dto.DashboardDtos.SerieVendas;
import com.umc.pediupatrao.dto.DashboardDtos.SituacaoPedidos;
import com.umc.pediupatrao.dto.DashboardDtos.VendaHorario;
import com.umc.pediupatrao.dto.DashboardDtos.VendaTipo;
import com.umc.pediupatrao.service.DashboardService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/resumo")
    public RespostaDashboard<ResumoVendas> resumo(FiltroDashboard filtro) {
        return dashboardService.resumo(filtro);
    }

    @GetMapping("/vendas")
    public RespostaDashboard<SerieVendas> vendas(FiltroDashboard filtro,
            @RequestParam(name = "agrupamento", defaultValue = "DIA") String agrupamento) {
        return dashboardService.vendas(filtro, agrupamento);
    }

    @GetMapping("/vendas/por-horario")
    public RespostaDashboard<List<VendaHorario>> vendasPorHorario(FiltroDashboard filtro) {
        return dashboardService.vendasPorHorario(filtro);
    }

    @GetMapping("/vendas/por-tipo")
    public RespostaDashboard<List<VendaTipo>> vendasPorTipo(FiltroDashboard filtro) {
        return dashboardService.vendasPorTipo(filtro);
    }

    @GetMapping("/pedidos/status")
    public RespostaDashboard<SituacaoPedidos> situacaoPedidos(FiltroDashboard filtro) {
        return dashboardService.situacaoPedidos(filtro);
    }

    @GetMapping("/produtos/mais-vendidos")
    public RespostaDashboard<List<ProdutoRanking>> produtosMaisVendidos(FiltroDashboard filtro,
            @RequestParam(name = "limite", defaultValue = "10") int limite) {
        return dashboardService.produtosMaisVendidos(filtro, limite);
    }

    @GetMapping("/pedidos")
    public RespostaDashboard<ListaPedidos> pedidos(FiltroDashboard filtro,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "20") int tamanho) {
        return dashboardService.listarPedidos(filtro, pagina, tamanho);
    }

    @GetMapping("/filtros")
    public OpcoesFiltro filtros() {
        return dashboardService.opcoesFiltro();
    }
}
