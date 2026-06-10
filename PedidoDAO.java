package com.kds.confeitaria.model;

/**
 * Formas de pagamento aceitas pela confeitaria.
 */
public enum FormaPagamento {

    PIX("Pix"),
    DINHEIRO("Dinheiro"),
    CARTAO_CREDITO("Cartão de Crédito"),
    CARTAO_DEBITO("Cartão de Débito");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
