package com.kds.confeitaria.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Representa um produto disponível no cardápio da confeitaria.
 * Cada produto pertence a uma {@link CategoriaProducao}, que por sua vez
 * define a {@link AreaProducao} responsável pela sua preparação.
 */
public class Produto {

    private int id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private CategoriaProducao categoria;
    private boolean ativo;

    public Produto() {
        this.ativo = true;
        this.preco = BigDecimal.ZERO;
    }

    public Produto(int id, String nome, String descricao, BigDecimal preco,
                   CategoriaProducao categoria, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.categoria = categoria;
        this.ativo = ativo;
    }

    // ---- Getters e Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public CategoriaProducao getCategoria() { return categoria; }
    public void setCategoria(CategoriaProducao categoria) { this.categoria = categoria; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    /**
     * Retorna a área de produção do produto com base em sua categoria.
     */
    public AreaProducao getAreaProducao() {
        return categoria != null ? categoria.getArea() : AreaProducao.GERAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Produto produto)) return false;
        return id == produto.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nome + " - R$ " + preco.toPlainString();
    }
}
