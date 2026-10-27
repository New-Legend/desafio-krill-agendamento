package br.com.redekrill.agendamento.infrastructure.erp;

import br.com.redekrill.agendamento.application.port.PedidoCompraGateway;
import br.com.redekrill.agendamento.domain.model.PedidoCompraErp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador local (padrão): pedidos de demonstração em memória, usado em testes/demo
 * enquanto não há credenciais do Bluesoft. Ative o real com ERP_PROVEDOR=bluesoft.
 */
@Component
@ConditionalOnProperty(name = "app.erp.provedor", havingValue = "local", matchIfMissing = true)
public class PedidoCompraLocalGateway implements PedidoCompraGateway {

    private static final Map<Long, PedidoCompraErp> PEDIDOS = Map.of(
            450001L, new PedidoCompraErp(450001L, "APROVADO", "Distribuidora Litoral Alimentos Ltda", "12.345.678/0001-90",
                    "agenda@litoralalimentos.com.br", 22, true, "CD 806", LocalDate.now().plusDays(2), "local"),
            450002L, new PedidoCompraErp(450002L, "APROVADO", "Bebidas Serra Azul S.A.", "23.456.789/0001-01",
                    "logistica@serraazul.com.br", 30, false, "CD 806", LocalDate.now().plusDays(3), "local"),
            450003L, new PedidoCompraErp(450003L, "APROVADO", "Higiene Total Indústria", "34.567.890/0001-12",
                    "expedicao@higienetotal.ind.br", 12, true, "CD 806", LocalDate.now().plusDays(1), "local"));

    @Override
    public Optional<PedidoCompraErp> buscarPedido(Long idPedido) {
        return Optional.ofNullable(PEDIDOS.get(idPedido));
    }

    @Override
    public String provedor() {
        return "local";
    }
}
