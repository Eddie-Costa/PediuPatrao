package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    public List<Produto> listarProdutosDisponiveis() {
        return produtoRepository.findAll().stream()
                .filter(produto -> !Boolean.FALSE.equals(produto.getAtivo()))
                .toList();
    }

    public Optional<Produto> buscarPorId(String id) {
        return produtoRepository.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public void excluir(String id) {
        produtoRepository.deleteById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public Produto salvar(Produto produto) {
        validar(produto);
        if (produto.getId() != null && produto.getId().isBlank()) {
            produto.setId(null);
        }
        if (produto.getId() != null) {
            Produto existente = produtoRepository.findById(produto.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Produto nao encontrado para atualizacao."));
            if (produto.getAtivo() == null) {
                produto.setAtivo(!Boolean.FALSE.equals(existente.getAtivo()));
            }
        } else if (produto.getAtivo() == null) {
            produto.setAtivo(true);
        }
        return produtoRepository.save(produto);
    }

    private void validar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new IllegalArgumentException("Informe o nome ou sabor do produto.");
        }
        if (produto.getTipo() == null || produto.getTipo().isBlank()) {
            throw new IllegalArgumentException("Selecione o tipo do produto.");
        }
        if (produto.getPreco() == null || !Double.isFinite(produto.getPreco()) || produto.getPreco() < 0) {
            throw new IllegalArgumentException("Informe um preco valido para o produto.");
        }
    }
}
