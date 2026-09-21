package com.sistemaempresarial.vendas.model;

/**
 * Categoria fiscal do produto e sua ALÍQUOTA BASE de imposto.
 * Manter a alíquota junto do tipo evita "if/switch" espalhados pelo código
 * e centraliza a regra fiscal em um único ponto de manutenção.
 */
public enum TipoProduto {
    ALIMENTACAO(0.010),      // 1,0%
    SAUDE_BEM_ESTAR(0.015),  // 1,5%
    VESTUARIO(0.025),        // 2,5%
    CULTURA(0.040);          // 4,0%

    private final double aliquota;

    TipoProduto(double aliquota) { this.aliquota = aliquota; }

    /** Alíquota base, antes de qualquer ajuste estadual. */
    public double aliquotaBase() { return aliquota; }
}