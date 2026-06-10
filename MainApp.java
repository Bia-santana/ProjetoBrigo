package com.kds.confeitaria.dao;

import com.kds.confeitaria.model.HistoricoStatus;
import com.kds.confeitaria.model.StatusPedido;
import com.kds.confeitaria.util.ConexaoBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Camada de acesso a dados para a entidade {@link HistoricoStatus}.
 * Garante a rastreabilidade completa de todas as transições de status dos pedidos.
 */
public class HistoricoStatusDAO {

    private static final String SQL_INSERIR = """
            INSERT INTO historico_status (pedido_id, status_de, status_para, alterado_em, observacao)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SQL_LISTAR_POR_PEDIDO = """
            SELECT id, pedido_id, status_de, status_para, alterado_em, observacao
            FROM historico_status
            WHERE pedido_id = ?
            ORDER BY alterado_em ASC
            """;

    /**
     * Registra uma nova transição de status no histórico.
     */
    public void registrar(HistoricoStatus historico) {
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERIR,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, historico.getPedidoId());
            if (historico.getStatusDe() != null) {
                ps.setString(2, historico.getStatusDe().name());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, historico.getStatusPara().name());
            ps.setObject(4, historico.getAlteradoEm() != null
                    ? historico.getAlteradoEm() : LocalDateTime.now());
            ps.setString(5, historico.getObservacao());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) historico.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao registrar histórico de status.", e);
        }
    }

    /**
     * Retorna o histórico completo de transições de um pedido.
     */
    public List<HistoricoStatus> listarPorPedido(int pedidoId) {
        List<HistoricoStatus> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_POR_PEDIDO)) {
            ps.setInt(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistoricoStatus h = new HistoricoStatus();
                    h.setId(rs.getInt("id"));
                    h.setPedidoId(rs.getInt("pedido_id"));
                    String de = rs.getString("status_de");
                    if (de != null) h.setStatusDe(StatusPedido.valueOf(de));
                    h.setStatusPara(StatusPedido.valueOf(rs.getString("status_para")));
                    h.setAlteradoEm(rs.getObject("alterado_em", LocalDateTime.class));
                    h.setObservacao(rs.getString("observacao"));
                    lista.add(h);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar histórico do pedido #" + pedidoId, e);
        }
        return lista;
    }
}
