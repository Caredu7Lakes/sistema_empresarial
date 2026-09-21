package com.sistemaempresarial.vendas.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Produto vendável. Cada produto sabe calcular o PRÓPRIO imposto
 * (princípio "tell, don't ask"), a partir da alíquota do seu tipo
 * mais um ajuste estadual (acréscimo/desconto por origem ou destino).
 *
 * Valores monetários usam BigDecimal — NUNCA double — para evitar
 * erros de arredondamento em cálculo de dinheiro/tributo.
 */
public class Produto {

    private final String codigoBarras;
    private final String descricao;
    private final BigDecimal valorVenda;
    private final TipoProduto tipo;

    /**
     * Ajuste sobre a alíquota conforme o estado de origem/destino.
     * Ex.: +0.005 = acréscimo de 0,5 p.p.; -0.003 = desconto de 0,3 p.p.
     */
    private final double ajusteEstadual;

    public Produto(String codigoBarras, String descricao, BigDecimal valorVenda,
                   TipoProduto tipo, double ajusteEstadual) {
        if (valorVenda == null || valorVenda.signum() < 0)
            throw new IllegalArgumentException("Valor de venda inválido.");
        if (tipo == null)
            throw new IllegalArgumentException("Tipo obrigatório.");
        this.codigoBarras = codigoBarras;
        this.descricao = descricao;
        this.valorVenda = valorVenda;
        this.tipo = tipo;
        this.ajusteEstadual = ajusteEstadual;
    }

    /**
     * Alíquota efetiva = base + ajuste, com PISO em zero:
     * um desconto maior que a base não pode gerar imposto negativo.
     */
    public double aliquotaEfetiva() {
        return Math.max(0.0, tipo.aliquotaBase() + ajusteEstadual);
    }

    /** Imposto do produto, arredondado a 2 casas (HALF_UP, padrão fiscal). */
    public BigDecimal calcularImposto() {
        return valorVenda.multiply(BigDecimal.valueOf(aliquotaEfetiva()))
                         .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal valorVenda() { return valorVenda; }
    public String codigoBarras()   { return codigoBarras; }
    public String descricao()      { return descricao; }
    public TipoProduto tipo()      { return tipo; }
}