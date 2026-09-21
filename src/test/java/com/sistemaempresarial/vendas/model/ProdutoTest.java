package com.sistemaempresarial.vendas.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testa o nó sensível de cálculo de IMPOSTO e suas bordas. */
class ProdutoTest {

    private Produto produto(BigDecimal valor, TipoProduto tipo, double ajuste) {
        return new Produto("cb", "x", valor, tipo, ajuste);
    }

    @Test @DisplayName("Alíquota base por tipo")
    void aliquotaPorTipo() {
        assertEquals(new BigDecimal("1.00"),
            produto(new BigDecimal("100.00"), TipoProduto.ALIMENTACAO, 0).calcularImposto());
        assertEquals(new BigDecimal("1.50"),
            produto(new BigDecimal("100.00"), TipoProduto.SAUDE_BEM_ESTAR, 0).calcularImposto());
        assertEquals(new BigDecimal("2.50"),
            produto(new BigDecimal("100.00"), TipoProduto.VESTUARIO, 0).calcularImposto());
        assertEquals(new BigDecimal("4.00"),
            produto(new BigDecimal("100.00"), TipoProduto.CULTURA, 0).calcularImposto());
    }

    @Test @DisplayName("Acréscimo estadual aumenta o imposto")
    void acrescimoEstadual() {
        // 1,0% + 0,5 p.p. = 1,5% de 100 = 1,50
        assertEquals(new BigDecimal("1.50"),
            produto(new BigDecimal("100.00"), TipoProduto.ALIMENTACAO, +0.005).calcularImposto());
    }

    @Test @DisplayName("Desconto estadual reduz o imposto")
    void descontoEstadual() {
        // 4,0% - 1,0 p.p. = 3,0% de 100 = 3,00
        assertEquals(new BigDecimal("3.00"),
            produto(new BigDecimal("100.00"), TipoProduto.CULTURA, -0.010).calcularImposto());
    }

    @Test @DisplayName("Piso em zero: desconto > base não gera imposto negativo")
    void pisoEmZero() {
        Produto p = produto(new BigDecimal("100.00"), TipoProduto.ALIMENTACAO, -0.05);
        assertEquals(0.0, p.aliquotaEfetiva());
        assertEquals(new BigDecimal("0.00"), p.calcularImposto());
    }

    @Test @DisplayName("Valor de venda negativo é rejeitado")
    void valorNegativo() {
        assertThrows(IllegalArgumentException.class,
            () -> produto(new BigDecimal("-1.00"), TipoProduto.CULTURA, 0));
    }

    @Test @DisplayName("Arredondamento HALF_UP a 2 casas")
    void arredondamento() {
        // 33.33 * 4% = 1.3332 -> 1.33
        assertEquals(new BigDecimal("1.33"),
            produto(new BigDecimal("33.33"), TipoProduto.CULTURA, 0).calcularImposto());
    }
}
