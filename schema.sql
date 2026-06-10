package com.kds.confeitaria.service;

import com.kds.confeitaria.dao.HistoricoStatusDAO;
import com.kds.confeitaria.dao.PedidoDAO;
import com.kds.confeitaria.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Camada de serviço responsável pela lógica de negócio dos pedidos.
 *
 * <p>Centraliza as regras de:</p>
 * <ul>
 *   <li>Criação e validação de pedidos</li>
 *   <li>Progressão de status com rastreabilidade</li>
 *   <li>Fila de prioridades (bolos de festa têm prioridade por horário)</li>
 *   <li>Filtros por área de produção</li>
 *   <li>Relatório de desempenho</li>
 * </ul>
 */
public class PedidoService {

    private final PedidoDAO pedidoDAO;
    private final HistoricoStatusDAO historicoDAO;

    // Contador para geração de número de pedido sequencial na sessão
    private static final AtomicInteger contadorPedido = new AtomicInteger(1);

    public PedidoService() {
        this.pedidoDAO = new PedidoDAO();
        this.historicoDAO = new HistoricoStatusDAO();
    }

    // ---- Criação de Pedidos ----

    /**
     * Cria e persiste um novo pedido, gerando automaticamente o número sequencial.
     *
     * @param nomeCliente    Nome do cliente
     * @param observacoes    Observações gerais (ex: "Sem glúten")
     * @param formaPagamento Forma de pagamento escolhida
     * @param horarioLimite  Prazo de entrega (pode ser null para pedidos sem prazo)
     * @param itens          Lista de itens do pedido
     * @return Pedido persistido com id gerado
     */
    public Pedido criarPedido(String nomeCliente, String observacoes,
                              FormaPagamento formaPagamento,
                              LocalDateTime horarioLimite,
                              List<ItemPedido> itens) {
        validarItens(itens);

        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(gerarNumeroPedido());
        pedido.setNomeCliente(nomeCliente.trim());
        pedido.setObservacoes(observacoes);
        pedido.setFormaPagamento(formaPagamento);
        pedido.setHorarioLimite(horarioLimite);
        pedido.setStatus(StatusPedido.PENDENTE);
        pedido.setItens(new ArrayList<>(itens));
        pedido.recalcularTotal();
        pedido.atualizarPrioridade();

        pedidoDAO.salvar(pedido);

        // Registra o status inicial no histórico
        historicoDAO.registrar(new HistoricoStatus(
                pedido.getId(), null, StatusPedido.PENDENTE, "Pedido criado"));

        return pedido;
    }

    // ---- Progressão de Status ----

    /**
     * Avança o status do pedido para o próximo estado válido na sequência.
     * Registra a transição no histórico de rastreabilidade.
     *
     * @param pedido Pedido a ser avançado
     * @throws IllegalStateException se o pedido não puder ser avançado
     */
    public void avancarStatus(Pedido pedido) {
        StatusPedido statusAtual = pedido.getStatus();
        StatusPedido proximoStatus = statusAtual.proximo();

        if (proximoStatus == null) {
            throw new IllegalStateException(
                    "O pedido #" + pedido.getNumeroPedido() + " já está no status final: " + statusAtual);
        }

        // Atualiza timestamps conforme o novo status
        LocalDateTime agora = LocalDateTime.now();
        if (proximoStatus == StatusPedido.EM_PREPARO) {
            pedido.setIniciadoEm(agora);
        } else if (proximoStatus == StatusPedido.PRONTO) {
            pedido.setFinalizadoEm(agora);
        } else if (proximoStatus == StatusPedido.ENTREGUE) {
            pedido.setEntregueEm(agora);
        }

        pedido.setStatus(proximoStatus);
        pedidoDAO.atualizarStatus(pedido);

        historicoDAO.registrar(new HistoricoStatus(
                pedido.getId(), statusAtual, proximoStatus,
                "Status avançado automaticamente"));
    }

    /**
     * Cancela um pedido, impedindo que seja avançado novamente.
     *
     * @param pedido     Pedido a ser cancelado
     * @param motivo     Motivo do cancelamento
     */
    public void cancelarPedido(Pedido pedido, String motivo) {
        if (pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new IllegalStateException("Não é possível cancelar um pedido já entregue.");
        }
        StatusPedido statusAnterior = pedido.getStatus();
        pedido.setStatus(StatusPedido.CANCELADO);
        pedidoDAO.atualizarStatus(pedido);

        historicoDAO.registrar(new HistoricoStatus(
                pedido.getId(), statusAnterior, StatusPedido.CANCELADO,
                motivo != null ? motivo : "Cancelado pelo operador"));
    }

    // ---- Consultas e Filtros ----

    /**
     * Retorna a fila de pedidos ativos ordenada por prioridade e horário limite.
     * Atualiza a prioridade de cada pedido antes de retornar.
     */
    public List<Pedido> getFilaPrioridade() {
        List<Pedido> pedidos = pedidoDAO.listarAtivos();
        pedidos.forEach(Pedido::atualizarPrioridade);
        Collections.sort(pedidos);
        return pedidos;
    }

    /**
     * Retorna pedidos ativos filtrados por status.
     */
    public List<Pedido> getPedidosPorStatus(StatusPedido status) {
        return pedidoDAO.listarPorStatus(status);
    }

    /**
     * Retorna pedidos ativos filtrados por área de produção.
     * Útil para separar a visão dos Fornos da visão dos Confeiteiros.
     */
    public List<Pedido> getPedidosPorArea(AreaProducao area) {
        return getFilaPrioridade().stream()
                .filter(p -> p.getItens().stream()
                        .anyMatch(i -> i.getAreaProducao() == area))
                .collect(Collectors.toList());
    }

    /**
     * Retorna o histórico de status de um pedido específico.
     */
    public List<HistoricoStatus> getHistorico(Pedido pedido) {
        return historicoDAO.listarPorPedido(pedido.getId());
    }

    /**
     * Gera o relatório de desempenho para o período informado.
     *
     * @param inicio Início do período
     * @param fim    Fim do período
     * @return Lista de arrays com dados agregados por dia
     */
    public List<Object[]> getRelatorioDesempenho(LocalDateTime inicio, LocalDateTime fim) {
        return pedidoDAO.relatorioDesempenho(inicio, fim);
    }

    // ---- Métodos auxiliares ----

    private String gerarNumeroPedido() {
        // Formato: KDS-YYYYMMDD-NNNN
        String data = LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("KDS-%s-%04d", data, contadorPedido.getAndIncrement());
    }

    private void validarItens(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Um pedido deve conter ao menos um item.");
        }
        for (ItemPedido item : itens) {
            if (item.getProduto() == null) {
                throw new IllegalArgumentException("Todos os itens devem ter um produto associado.");
            }
            if (item.getQuantidade() <= 0) {
                throw new IllegalArgumentException(
                        "A quantidade do item '" + item.getProduto().getNome() + "' deve ser maior que zero.");
            }
        }
    }
}
