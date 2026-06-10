package com.kds.confeitaria.dao;

import com.kds.confeitaria.model.*;
import com.kds.confeitaria.util.ConexaoBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Camada de acesso a dados para a entidade {@link Pedido}.
 * Responsável por persistir, atualizar e consultar pedidos e seus itens.
 */
public class PedidoDAO {

    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    // ---- SQL ----

    private static final String SQL_INSERIR_PEDIDO = """
            INSERT INTO pedido (numero_pedido, nome_cliente, observacoes, status,
                                prioridade, forma_pagamento, valor_total, horario_limite, criado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_INSERIR_ITEM = """
            INSERT INTO item_pedido (pedido_id, produto_id, quantidade, preco_unitario, observacao)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SQL_ATUALIZAR_STATUS = """
            UPDATE pedido
            SET status = ?,
                iniciado_em    = CASE WHEN ? = 'EM_PREPARO' THEN ? ELSE iniciado_em END,
                finalizado_em  = CASE WHEN ? = 'PRONTO'     THEN ? ELSE finalizado_em END,
                entregue_em    = CASE WHEN ? = 'ENTREGUE'   THEN ? ELSE entregue_em END,
                tempo_preparo_min = ?
            WHERE id = ?
            """;

    private static final String SQL_LISTAR_ATIVOS = """
            SELECT p.id, p.numero_pedido, p.nome_cliente, p.observacoes, p.status,
                   p.prioridade, p.forma_pagamento, p.valor_total, p.horario_limite,
                   p.criado_em, p.iniciado_em, p.finalizado_em, p.entregue_em, p.tempo_preparo_min
            FROM pedido p
            WHERE p.status NOT IN ('ENTREGUE', 'CANCELADO')
            ORDER BY p.prioridade DESC, p.horario_limite ASC, p.criado_em ASC
            """;

    private static final String SQL_LISTAR_POR_STATUS = """
            SELECT p.id, p.numero_pedido, p.nome_cliente, p.observacoes, p.status,
                   p.prioridade, p.forma_pagamento, p.valor_total, p.horario_limite,
                   p.criado_em, p.iniciado_em, p.finalizado_em, p.entregue_em, p.tempo_preparo_min
            FROM pedido p
            WHERE p.status = ?
            ORDER BY p.prioridade DESC, p.horario_limite ASC, p.criado_em ASC
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT p.id, p.numero_pedido, p.nome_cliente, p.observacoes, p.status,
                   p.prioridade, p.forma_pagamento, p.valor_total, p.horario_limite,
                   p.criado_em, p.iniciado_em, p.finalizado_em, p.entregue_em, p.tempo_preparo_min
            FROM pedido p
            WHERE p.id = ?
            """;

    private static final String SQL_LISTAR_ITENS = """
            SELECT ip.id, ip.produto_id, ip.quantidade, ip.preco_unitario, ip.observacao
            FROM item_pedido ip
            WHERE ip.pedido_id = ?
            """;

    private static final String SQL_RELATORIO_DESEMPENHO = """
            SELECT DATE(criado_em) AS data,
                   COUNT(*) AS total_pedidos,
                   AVG(tempo_preparo_min) AS media_preparo_min,
                   MIN(tempo_preparo_min) AS min_preparo_min,
                   MAX(tempo_preparo_min) AS max_preparo_min,
                   SUM(valor_total) AS faturamento
            FROM pedido
            WHERE status IN ('PRONTO', 'ENTREGUE')
              AND criado_em BETWEEN ? AND ?
            GROUP BY DATE(criado_em)
            ORDER BY data DESC
            """;

    // ---- Operações de escrita ----

    /**
     * Persiste um novo pedido e todos os seus itens no banco de dados.
     * Utiliza transação para garantir consistência.
     *
     * @param pedido Pedido a ser salvo (id será preenchido após inserção)
     */
    public void salvar(Pedido pedido) {
        try (Connection conn = ConexaoBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                inserirPedido(conn, pedido);
                for (ItemPedido item : pedido.getItens()) {
                    inserirItem(conn, pedido.getId(), item);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar pedido.", e);
        }
    }

    /**
     * Atualiza o status de um pedido e registra os timestamps correspondentes.
     *
     * @param pedido Pedido com o novo status já definido
     */
    public void atualizarStatus(Pedido pedido) {
        LocalDateTime agora = LocalDateTime.now();
        String novoStatus = pedido.getStatus().name();

        // Calcula tempo de preparo ao finalizar
        Integer tempoMin = null;
        if (pedido.getStatus() == StatusPedido.PRONTO && pedido.getIniciadoEm() != null) {
            long diff = java.time.temporal.ChronoUnit.MINUTES.between(pedido.getIniciadoEm(), agora);
            tempoMin = (int) diff;
            pedido.setTempoPreparoMin(tempoMin);
        }

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_ATUALIZAR_STATUS)) {
            ps.setString(1, novoStatus);
            // Parâmetros para CASE EM_PREPARO
            ps.setString(2, novoStatus);
            ps.setObject(3, novoStatus.equals("EM_PREPARO") ? agora : null);
            // Parâmetros para CASE PRONTO
            ps.setString(4, novoStatus);
            ps.setObject(5, novoStatus.equals("PRONTO") ? agora : null);
            // Parâmetros para CASE ENTREGUE
            ps.setString(6, novoStatus);
            ps.setObject(7, novoStatus.equals("ENTREGUE") ? agora : null);
            // Tempo de preparo
            if (tempoMin != null) ps.setInt(8, tempoMin);
            else ps.setNull(8, Types.INTEGER);
            ps.setInt(9, pedido.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar status do pedido #" + pedido.getId(), e);
        }
    }

    // ---- Operações de leitura ----

    /**
     * Retorna todos os pedidos que ainda estão em produção (não entregues/cancelados).
     */
    public List<Pedido> listarAtivos() {
        return listarComSQL(SQL_LISTAR_ATIVOS, null);
    }

    /**
     * Retorna pedidos filtrados por um status específico.
     */
    public List<Pedido> listarPorStatus(StatusPedido status) {
        return listarComSQL(SQL_LISTAR_POR_STATUS, status.name());
    }

    /**
     * Busca um pedido pelo seu identificador, incluindo todos os itens.
     */
    public Optional<Pedido> buscarPorId(int id) {
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Pedido pedido = mapearPedido(rs);
                    pedido.setItens(buscarItensDoPedido(conn, pedido.getId()));
                    return Optional.of(pedido);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar pedido por ID: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Retorna dados agregados de desempenho para o período informado.
     * Cada linha do resultado representa um dia com totais e médias.
     *
     * @param inicio Data/hora de início do período
     * @param fim    Data/hora de fim do período
     * @return Lista de arrays: [data, total, media_min, min_min, max_min, faturamento]
     */
    public List<Object[]> relatorioDesempenho(LocalDateTime inicio, LocalDateTime fim) {
        List<Object[]> resultado = new ArrayList<>();
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_RELATORIO_DESEMPENHO)) {
            ps.setObject(1, inicio);
            ps.setObject(2, fim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new Object[]{
                            rs.getDate("data"),
                            rs.getInt("total_pedidos"),
                            rs.getDouble("media_preparo_min"),
                            rs.getInt("min_preparo_min"),
                            rs.getInt("max_preparo_min"),
                            rs.getBigDecimal("faturamento")
                    });
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao gerar relatório de desempenho.", e);
        }
        return resultado;
    }

    // ---- Métodos auxiliares privados ----

    private void inserirPedido(Connection conn, Pedido pedido) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR_PEDIDO,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, pedido.getNumeroPedido());
            ps.setString(2, pedido.getNomeCliente());
            ps.setString(3, pedido.getObservacoes());
            ps.setString(4, pedido.getStatus().name());
            ps.setInt(5, pedido.getPrioridade());
            ps.setString(6, pedido.getFormaPagamento().name());
            ps.setBigDecimal(7, pedido.getValorTotal());
            ps.setObject(8, pedido.getHorarioLimite());
            ps.setObject(9, pedido.getCriadoEm());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) pedido.setId(keys.getInt(1));
            }
        }
    }

    private void inserirItem(Connection conn, int pedidoId, ItemPedido item) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR_ITEM,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pedidoId);
            ps.setInt(2, item.getProduto().getId());
            ps.setInt(3, item.getQuantidade());
            ps.setBigDecimal(4, item.getPrecoUnitario());
            ps.setString(5, item.getObservacao());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
        }
    }

    private List<Pedido> listarComSQL(String sql, String parametro) {
        List<Pedido> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parametro != null) ps.setString(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pedido p = mapearPedido(rs);
                    p.setItens(buscarItensDoPedido(conn, p.getId()));
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar pedidos.", e);
        }
        return lista;
    }

    private List<ItemPedido> buscarItensDoPedido(Connection conn, int pedidoId) throws SQLException {
        List<ItemPedido> itens = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_ITENS)) {
            ps.setInt(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemPedido item = new ItemPedido();
                    item.setId(rs.getInt("id"));
                    item.setQuantidade(rs.getInt("quantidade"));
                    item.setPrecoUnitario(rs.getBigDecimal("preco_unitario"));
                    item.setObservacao(rs.getString("observacao"));
                    produtoDAO.buscarPorId(rs.getInt("produto_id"))
                            .ifPresent(item::setProduto);
                    itens.add(item);
                }
            }
        }
        return itens;
    }

    private Pedido mapearPedido(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getInt("id"));
        p.setNumeroPedido(rs.getString("numero_pedido"));
        p.setNomeCliente(rs.getString("nome_cliente"));
        p.setObservacoes(rs.getString("observacoes"));
        p.setStatus(StatusPedido.valueOf(rs.getString("status")));
        p.setPrioridade(rs.getInt("prioridade"));
        p.setFormaPagamento(FormaPagamento.valueOf(rs.getString("forma_pagamento")));
        p.setValorTotal(rs.getBigDecimal("valor_total"));
        p.setHorarioLimite(rs.getObject("horario_limite", LocalDateTime.class));
        p.setCriadoEm(rs.getObject("criado_em", LocalDateTime.class));
        p.setIniciadoEm(rs.getObject("iniciado_em", LocalDateTime.class));
        p.setFinalizadoEm(rs.getObject("finalizado_em", LocalDateTime.class));
        p.setEntregueEm(rs.getObject("entregue_em", LocalDateTime.class));
        int tempo = rs.getInt("tempo_preparo_min");
        if (!rs.wasNull()) p.setTempoPreparoMin(tempo);
        return p;
    }
}
