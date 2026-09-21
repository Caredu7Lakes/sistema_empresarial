package com.sistemaempresarial.vendas.service;

import com.sistemaempresarial.vendas.model.Produto;

import java.util.Map;

/**
 * Módulo de entrada por CÓDIGO DE BARRAS.
 * Traduz o código lido em um Produto usando um catálogo.
 * Aqui o catálogo é um Map em memória; em produção seria um repositório
 * sobre RDS PostgreSQL — a troca não afeta o Caixa (depende só desta classe).
 */
public class LeitorCodigoBarras {

    private final Map<String, Produto> catalogo;

    public LeitorCodigoBarras(Map<String, Produto> catalogo) {
        this.catalogo = catalogo;
    }

    /** Retorna o produto do código lido; lança exceção se não cadastrado. */
    public Produto ler(String codigoBarras) {
        Produto p = catalogo.get(codigoBarras);
        if (p == null)
            throw new IllegalArgumentException("Produto não encontrado: " + codigoBarras);
        return p;
    }
}
