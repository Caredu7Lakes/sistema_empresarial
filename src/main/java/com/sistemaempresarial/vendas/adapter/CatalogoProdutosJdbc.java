package com.sistemaempresarial.vendas.adapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

import com.sistemaempresarial.vendas.model.Produto;
import com.sistemaempresarial.vendas.port.CatalogoProdutos;

public class CatalogoProdutosJdbc implements CatalogoProdutos {

    private static final String SQL_POR_CODIGO =
        "SELECT codigo_barras, descricao, tipo, valor_venda, ajuste_estadual " +
        "FROM produto WHERE codigo_barras = ?";

    private final DataSource dataSource;

    public CatalogoProdutosJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Produto> buscarPorCodigoBarras(String codigoBarras) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_POR_CODIGO)) {

            ps.setString(1, codigoBarras);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(ProdutoMapper.mapear(
                        rs.getString("codigo_barras"),
                        rs.getString("descricao"),
                        rs.getString("tipo"),
                        rs.getBigDecimal("valor_venda"),
                        rs.getBigDecimal("ajuste_estadual")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao consultar produto: " + codigoBarras, e);
        }
    }
}
