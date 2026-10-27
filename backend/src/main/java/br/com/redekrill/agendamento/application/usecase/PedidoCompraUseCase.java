package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.port.PedidoCompraGateway;
import br.com.redekrill.agendamento.application.port.TipoPaletizacaoRepositorio;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.PedidoCompraErp;
import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;

/** Consulta o pedido no ERP para preencher o formulário de agendamento. */
public class PedidoCompraUseCase {

    public record Sugestao(PedidoCompraErp pedido, Long tipoPaletizacaoId, String provedor) {
    }

    private final PedidoCompraGateway gateway;
    private final TipoPaletizacaoRepositorio paletizacoes;

    public PedidoCompraUseCase(PedidoCompraGateway gateway, TipoPaletizacaoRepositorio paletizacoes) {
        this.gateway = gateway;
        this.paletizacoes = paletizacoes;
    }

    public Sugestao consultar(Long idPedido) {
        if (idPedido == null || idPedido <= 0) throw new RegraNegocioException("ID do pedido deve ser um número positivo");
        PedidoCompraErp p = gateway.buscarPedido(idPedido)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido no ERP (" + gateway.provedor() + ")", idPedido));
        Long tipo = null;
        if (p.paletizado() != null) {
            int fator = p.paletizado() ? 1 : 2;
            tipo = paletizacoes.listar().stream().filter(TipoPaletizacao::ativo)
                    .filter(t -> t.fatorMultiplicador() == fator).map(TipoPaletizacao::id).findFirst().orElse(null);
        }
        return new Sugestao(p, tipo, gateway.provedor());
    }
}
