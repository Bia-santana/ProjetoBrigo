package com.kds.confeitaria.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registra cada transição de status de um pedido, garantindo
 * rastreabilidade completa do fluxo de produção.
 */
public class HistoricoStatus {

    private int id;
    private int pedidoId;
    private StatusPedido statusDe;
    private StatusPedido statusPara;
    private LocalDateTime alteradoEm;
    private String observacao;

    public HistoricoStatus() {
        this.alteradoEm = LocalDateTime.now();
    }

    public HistoricoStatus(int pedidoId, StatusPedido statusDe,
                           StatusPedido statusPara, String observacao) {
        this.pedidoId = pedidoId;
        this.statusDe = statusDe;
        this.statusPara = statusPara;
        this.observacao = observacao;
        this.alteradoEm = LocalDateTime.now();
    }

    // ---- Getters e Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPedidoId() { return pedidoId; }
    public void setPedidoId(int pedidoId) { this.pedidoId = pedidoId; }

    public StatusPedido getStatusDe() { return statusDe; }
    public void setStatusDe(StatusPedido statusDe) { this.statusDe = statusDe; }

    public StatusPedido getStatusPara() { return statusPara; }
    public void setStatusPara(StatusPedido statusPara) { this.statusPara = statusPara; }

    public LocalDateTime getAlteradoEm() { return alteradoEm; }
    public void setAlteradoEm(LocalDateTime alteradoEm) { this.alteradoEm = alteradoEm; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistoricoStatus that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Pedido #" + pedidoId + ": " + statusDe + " → " + statusPara
                + " em " + alteradoEm;
    }
}
