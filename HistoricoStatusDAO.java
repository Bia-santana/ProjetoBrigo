package com.kds.confeitaria.model;

import java.util.Objects;

/**
 * Representa uma categoria de produto (ex: Bolos, Tortas, Recheios).
 * Cada categoria pertence a uma {@link AreaProducao}, permitindo filtrar
 * os pedidos por estação de trabalho na tela do KDS.
 */
public class CategoriaProducao {

    private int id;
    private String nome;
    private String descricao;
    private AreaProducao area;

    public CategoriaProducao() {}

    public CategoriaProducao(int id, String nome, String descricao, AreaProducao area) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.area = area;
    }

    // ---- Getters e Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public AreaProducao getArea() { return area; }
    public void setArea(AreaProducao area) { this.area = area; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoriaProducao that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nome + " [" + area + "]";
    }
}
