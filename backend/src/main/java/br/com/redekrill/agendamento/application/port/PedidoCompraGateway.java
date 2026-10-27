package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.PedidoCompraErp;

import java.util.Optional;

/** Porta de integração com o ERP (Bluesoft). O caso de uso não sabe qual ERP está por trás. */
public interface PedidoCompraGateway {
    Optional<PedidoCompraErp> buscarPedido(Long idPedido);

    /** Nome do provedor ativo (ex.: "bluesoft", "local"), exibido na tela. */
    String provedor();
}
