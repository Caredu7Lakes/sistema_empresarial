package com.sistemaempresarial.vendas.port;

import java.util.Optional;
import com.sistemaempresarial.vendas.model.Produto;

public interface CatalogoProdutos {
    Optional<Produto> buscarPorCodigoBarras(String codigoBarras);
}
