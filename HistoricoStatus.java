package com.kds.confeitaria.model;

/**
 * Representa os possíveis estados de um pedido no fluxo da cozinha.
 * A progressão natural é: PENDENTE → EM_PREPARO → PRONTO → ENTREGUE.
 * O status CANCELADO pode ser atingido a partir de qualquer estado anterior a ENTREGUE.
 */
public enum StatusPedido {

    PENDENTE("Pendente", "#4FC3F7"),
    EM_PREPARO("Em Preparo", "#FFF176"),
    PRONTO("Pronto para Entrega", "#81C784"),
    ENTREGUE("Entregue", "#B0BEC5"),
    CANCELADO("Cancelado", "#EF9A9A");

    private final String descricao;
    private final String corHex;

    StatusPedido(String descricao, String corHex) {
        this.descricao = descricao;
        this.corHex = corHex;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCorHex() {
        return corHex;
    }

    /**
     * Retorna o próximo status válido na progressão do pedido.
     * Retorna null se não houver próximo estado (pedido já finalizado ou cancelado).
     */
    public StatusPedido proximo() {
        return switch (this) {
            case PENDENTE   -> EM_PREPARO;
            case EM_PREPARO -> PRONTO;
            case PRONTO     -> ENTREGUE;
            default         -> null;
        };
    }

    /**
     * Verifica se é possível avançar o status a partir do estado atual.
     */
    public boolean podeAvancar() {
        return proximo() != null;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
