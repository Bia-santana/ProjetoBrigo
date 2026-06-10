package com.kds.confeitaria.dao;

import com.kds.confeitaria.model.AreaProducao;
import com.kds.confeitaria.model.CategoriaProducao;
import com.kds.confeitaria.model.Produto;
import com.kds.confeitaria.util.ConexaoBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Camada de acesso a dados para a entidade {@link Produto}.
 * Utiliza JDBC puro para todas as operações com o banco MySQL.
 */
public class ProdutoDAO {

    // ---- SQL ----

    private static final String SQL_LISTAR_ATIVOS = """
            SELECT p.id, p.nome, p.descricao, p.preco, p.ativo,
                   c.id AS cat_id, c.nome AS cat_nome, c.descricao AS cat_desc, c.area
            FROM produto p
            JOIN categoria_produto c ON p.categoria_id = c.id
            WHERE p.ativo = 1
            ORDER BY c.area, p.nome
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT p.id, p.nome, p.descricao, p.preco, p.ativo,
                   c.id AS cat_id, c.nome AS cat_nome, c.descricao AS cat_desc, c.area
            FROM produto p
            JOIN categoria_produto c ON p.categoria_id = c.id
            WHERE p.id = ?
            """;

    private static final String SQL_LISTAR_POR_AREA = """
            SELECT p.id, p.nome, p.descricao, p.preco, p.ativo,
                   c.id AS cat_id, c.nome AS cat_nome, c.descricao AS cat_desc, c.area
            FROM produto p
            JOIN categoria_produto c ON p.categoria_id = c.id
            WHERE p.ativo = 1 AND c.area = ?
            ORDER BY p.nome
            """;

    // ---- Métodos públicos ----

    /**
     * Retorna todos os produtos ativos do cardápio.
     */
    public List<Produto> listarAtivos() {
        List<Produto> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_ATIVOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produtos ativos.", e);
        }
        return lista;
    }

    /**
     * Busca um produto pelo seu identificador.
     */
    public Optional<Produto> buscarPorId(int id) {
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar produto por ID: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Retorna os produtos ativos de uma área de produção específica.
     */
    public List<Produto> listarPorArea(AreaProducao area) {
        List<Produto> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_POR_AREA)) {
            ps.setString(1, area.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produtos por área: " + area, e);
        }
        return lista;
    }

    // ---- Mapeamento ResultSet → Objeto ----

    private Produto mapear(ResultSet rs) throws SQLException {
        CategoriaProducao cat = new CategoriaProducao(
                rs.getInt("cat_id"),
                rs.getString("cat_nome"),
                rs.getString("cat_desc"),
                AreaProducao.valueOf(rs.getString("area"))
        );
        return new Produto(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("descricao"),
                rs.getBigDecimal("preco"),
                cat,
                rs.getBoolean("ativo")
        );
    }
}
