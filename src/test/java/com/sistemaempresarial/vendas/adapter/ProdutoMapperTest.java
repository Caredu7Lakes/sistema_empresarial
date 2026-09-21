package com.sistemaempresarial.vendas.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sistemaempresarial.vendas.model.Produto;
import com.sistemaempresarial.vendas.model.TipoProduto;

class ProdutoMapperTest {

    @Test @DisplayName("Mapeia linha valida para Produto")
    void mapeiaValido() {
        Produto p = ProdutoMapper.mapear("789", "Arroz", "ALIMENTACAO",
                new BigDecimal("29.90"), new BigDecimal("0.0000"));
        assertEquals(TipoProduto.ALIMENTACAO, p.tipo());
        assertEquals(new BigDecimal("29.90"), p.valorVenda());
    }

    @Test @DisplayName("Ajuste estadual do banco e aplicado")
    void aplicaAjuste() {
        Produto p = ProdutoMapper.mapear("789", "Camiseta", "VESTUARIO",
                new BigDecimal("100.00"), new BigDecimal("0.0050"));
        assertEquals(new BigDecimal("3.00"), p.calcularImposto());
    }

    @Test @DisplayName("Ajuste nulo vira zero")
    void ajusteNulo() {
        Produto p = ProdutoMapper.mapear("789", "Livro", "CULTURA",
                new BigDecimal("50.00"), null);
        assertEquals(new BigDecimal("2.00"), p.calcularImposto());
    }

    @Test @DisplayName("Tipo desconhecido no banco e rejeitado")
    void tipoInvalido() {
        assertThrows(IllegalStateException.class,
            () -> ProdutoMapper.mapear("789", "X", "BEBIDAS",
                    new BigDecimal("10.00"), BigDecimal.ZERO));
    }
}
