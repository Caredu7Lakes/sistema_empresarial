package com.sistemaempresarial.vendas.service;

import com.sistemaempresarial.vendas.model.Produto;
import com.sistemaempresarial.vendas.port.CatalogoProdutos;

/**
 * Módulo de entrada por CÓDIGO DE BARRAS.
 * Depende da porta CatalogoProdutos — não conhece se a origem é memória ou banco.
 */
public class LeitorCodigoBarras {

    private final CatalogoProdutos catalogo;

    public LeitorCodigoBarras(CatalogoProdutos catalogo) {
        this.catalogo = catalogo;
    }

    /** Retorna o produto do código lido; lança exceção se não cadastrado. */
    public Produto ler(String codigoBarras) {
        return catalogo.buscarPorCodigoBarras(codigoBarras)
                .orElseThrow(() ->
                        new IllegalArgumentException("Produto não encontrado: " + codigoBarras));
    }
}