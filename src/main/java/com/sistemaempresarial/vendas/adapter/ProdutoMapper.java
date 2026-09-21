package com.sistemaempresarial.vendas.adapter;

import java.math.BigDecimal;
import com.sistemaempresarial.vendas.model.Produto;
import com.sistemaempresarial.vendas.model.TipoProduto;

final class ProdutoMapper {
    private ProdutoMapper() {}

    static Produto mapear(String codigoBarras, String descricao, String tipo,
                          BigDecimal valorVenda, BigDecimal ajusteEstadual) {
        TipoProduto tipoProduto;
        try {
            tipoProduto = TipoProduto.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Tipo invalido no banco: " + tipo, e);
        }
        double ajuste = ajusteEstadual == null ? 0.0 : ajusteEstadual.doubleValue();
        return new Produto(codigoBarras, descricao, valorVenda, tipoProduto, ajuste);
    }
}
