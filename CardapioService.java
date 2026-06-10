package com.kds.confeitaria.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Representa um item individual dentro de um pedido.
 * Armazena o produto solicitado, a quantidade, o preço unitário
 * no momento do pedido e observações específicas do cliente.
 */
public class ItemPedido {

    private int id;
    private Produto produto;
    private int quantidade;
    private BigDecimal precoUnitario;
    private String observacao;

    public ItemPedido() {
        this.quantidade = 1;
        this.precoUnitario = BigDecimal.ZERO;
    }

    public ItemPedido(Produto produto, int quantidade, String observacao) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto != null ? produto.getPreco() : BigDecimal.ZERO;
        this.observacao = observacao;
    }

    // ---- Getters e Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) {
        this.produto = produto;
        if (produto != null && (precoUnitario == null || precoUnitario.compareTo(BigDecimal.ZERO) == 0)) {
            this.precoUnitario = produto.getPreco();
        }
    }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    /**
     * Calcula o subtotal deste item (preço unitário × quantidade).
     */
    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /**
     * Retorna a área de produção responsável por este item.
     */
    public AreaProducao getAreaProducao() {
        return produto != null ? produto.getAreaProducao() : AreaProducao.GERAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemPedido that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String obs = (observacao != null && !observacao.isBlank()) ? " (" + observacao + ")" : "";
        return quantidade + "x " + (produto != null ? produto.getNome() : "?") + obs;
    }
}
