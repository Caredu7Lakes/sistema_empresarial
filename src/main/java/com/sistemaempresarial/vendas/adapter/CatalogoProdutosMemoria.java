package com.sistemaempresarial.vendas.adapter;

import java.util.Map;
import java.util.Optional;

import com.sistemaempresarial.vendas.model.Produto;
import com.sistemaempresarial.vendas.port.CatalogoProdutos;

public class CatalogoProdutosMemoria implements CatalogoProdutos {
    private final Map<String, Produto> mapa;
    public CatalogoProdutosMemoria(Map<String, Produto> mapa) { this.mapa = mapa; }

    @Override
    public Optional<Produto> buscarPorCodigoBarras(String codigoBarras) {
        return Optional.ofNullable(mapa.get(codigoBarras));
    }
}
