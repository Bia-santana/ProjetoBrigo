package com.kds.confeitaria.model;

/**
 * Representa as áreas de produção da confeitaria.
 * Utilizada para filtrar os pedidos exibidos no KDS por estação de trabalho.
 */
public enum AreaProducao {

    FORNOS("Fornos", "Responsável por bolos e tortas assadas"),
    CONFEITEIROS("Confeiteiros", "Responsável por recheios e decorações"),
    GERAL("Geral", "Itens sem área específica");

    private final String nome;
    private final String descricao;

    AreaProducao(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return nome;
    }
}
