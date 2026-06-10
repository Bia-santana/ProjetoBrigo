package com.kds.confeitaria.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidade central do sistema KDS. Representa um pedido de cliente
 * desde o momento em que é registrado até a sua entrega.
 *
 * <p>A prioridade é determinada automaticamente com base no horário
 * limite de entrega: pedidos com menos de 30 minutos restantes são
 * considerados urgentes; entre 30 e 60 minutos, alta prioridade.</p>
 */
public class Pedido implements Comparable<Pedido> {

    private int id;
    private String numeroPedido;
    private String nomeCliente;
    private String observacoes;
    private StatusPedido status;
    private int prioridade;           // 0=Normal, 1=Alta, 2=Urgente
    private FormaPagamento formaPagamento;
    private BigDecimal valorTotal;
    private LocalDateTime horarioLimite;
    private LocalDateTime criadoEm;
    private LocalDateTime iniciadoEm;
    private LocalDateTime finalizadoEm;
    private LocalDateTime entregueEm;
    private Integer tempoPreparoMin;
    private List<ItemPedido> itens;

    public Pedido() {
        this.status = StatusPedido.PENDENTE;
        this.formaPagamento = FormaPagamento.PIX;
        this.valorTotal = BigDecimal.ZERO;
        this.prioridade = 0;
        this.criadoEm = LocalDateTime.now();
        this.itens = new ArrayList<>();
    }

    // ---- Getters e Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }

    public int getPrioridade() { return prioridade; }
    public void setPrioridade(int prioridade) { this.prioridade = prioridade; }

    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public LocalDateTime getHorarioLimite() { return horarioLimite; }
    public void setHorarioLimite(LocalDateTime horarioLimite) { this.horarioLimite = horarioLimite; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getIniciadoEm() { return iniciadoEm; }
    public void setIniciadoEm(LocalDateTime iniciadoEm) { this.iniciadoEm = iniciadoEm; }

    public LocalDateTime getFinalizadoEm() { return finalizadoEm; }
    public void setFinalizadoEm(LocalDateTime finalizadoEm) { this.finalizadoEm = finalizadoEm; }

    public LocalDateTime getEntregueEm() { return entregueEm; }
    public void setEntregueEm(LocalDateTime entregueEm) { this.entregueEm = entregueEm; }

    public Integer getTempoPreparoMin() { return tempoPreparoMin; }
    public void setTempoPreparoMin(Integer tempoPreparoMin) { this.tempoPreparoMin = tempoPreparoMin; }

    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens; }

    // ---- Métodos de negócio ----

    /**
     * Adiciona um item ao pedido e recalcula o valor total.
     */
    public void adicionarItem(ItemPedido item) {
        itens.add(item);
        recalcularTotal();
    }

    /**
     * Remove um item do pedido e recalcula o valor total.
     */
    public void removerItem(ItemPedido item) {
        itens.remove(item);
        recalcularTotal();
    }

    /**
     * Recalcula o valor total somando os subtotais de todos os itens.
     */
    public void recalcularTotal() {
        valorTotal = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula os minutos restantes até o horário limite de entrega.
     * Retorna null se não houver horário limite definido.
     */
    public Long getMinutosRestantes() {
        if (horarioLimite == null) return null;
        return ChronoUnit.MINUTES.between(LocalDateTime.now(), horarioLimite);
    }

    /**
     * Determina se o pedido está atrasado (horário limite no passado).
     */
    public boolean isAtrasado() {
        Long restantes = getMinutosRestantes();
        return restantes != null && restantes < 0;
    }

    /**
     * Determina se o pedido é urgente (menos de 30 minutos para o limite).
     */
    public boolean isUrgente() {
        Long restantes = getMinutosRestantes();
        return restantes != null && restantes >= 0 && restantes < 30;
    }

    /**
     * Calcula e atualiza a prioridade com base no horário limite.
     * Deve ser chamado periodicamente pela camada de serviço.
     */
    public void atualizarPrioridade() {
        Long restantes = getMinutosRestantes();
        if (restantes == null) {
            prioridade = 0;
        } else if (restantes < 0 || restantes < 30) {
            prioridade = 2; // Urgente
        } else if (restantes < 60) {
            prioridade = 1; // Alta
        } else {
            prioridade = 0; // Normal
        }
    }

    /**
     * Retorna a cor CSS correspondente ao estado atual do pedido,
     * considerando atraso como situação prioritária.
     */
    public String getCorStatus() {
        if (isAtrasado() && status != StatusPedido.ENTREGUE && status != StatusPedido.CANCELADO) {
            return "#EF5350"; // Vermelho — atrasado
        }
        return status.getCorHex();
    }

    /**
     * Ordena pedidos por prioridade (desc) e depois por horário limite (asc).
     * Pedidos sem horário limite ficam por último.
     */
    @Override
    public int compareTo(Pedido outro) {
        int cmpPrioridade = Integer.compare(outro.prioridade, this.prioridade);
        if (cmpPrioridade != 0) return cmpPrioridade;

        if (this.horarioLimite == null && outro.horarioLimite == null) return 0;
        if (this.horarioLimite == null) return 1;
        if (outro.horarioLimite == null) return -1;
        return this.horarioLimite.compareTo(outro.horarioLimite);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pedido pedido)) return false;
        return id == pedido.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Pedido #" + numeroPedido + " [" + nomeCliente + "] - " + status;
    }
}
