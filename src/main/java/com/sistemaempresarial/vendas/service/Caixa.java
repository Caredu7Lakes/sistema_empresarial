package com.sistemaempresarial.vendas.service;

import com.sistemaempresarial.vendas.model.Produto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Caixa da venda: acumula os itens lidos e totaliza valor e imposto.
 * Delega a leitura ao LeitorCodigoBarras e o cálculo fiscal ao Produto,
 * mantendo baixa responsabilidade (apenas somatório).
 */
public class Caixa {

    private final LeitorCodigoBarras leitor;
    private final List<Produto> itens = new ArrayList<>();

    public Caixa(LeitorCodigoBarras leitor) { this.leitor = leitor; }

    /** Lê o produto pelo código e o adiciona à venda. */
    public Produto adicionarPorCodigo(String codigoBarras) {
        Produto p = leitor.ler(codigoBarras);
        itens.add(p);
        return p;
    }

    /** Soma dos valores de venda dos itens. */
    public BigDecimal totalVenda() {
        return itens.stream().map(Produto::valorVenda)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Soma dos impostos dos itens (cada um já com seu ajuste estadual). */
    public BigDecimal totalImposto() {
        return itens.stream().map(Produto::calcularImposto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
