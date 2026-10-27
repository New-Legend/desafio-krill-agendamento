package br.com.redekrill.agendamento.domain.model;

/**
 * Dados de um pedido de compra vindos do ERP (Bluesoft), usados para preencher o agendamento.
 * Campos ausentes no ERP chegam nulos.
 */
public record PedidoCompraErp(Long idPedido, String situacao, String fornecedorNome, String fornecedorCnpj,
                              String fornecedorEmail, Integer quantidadePaletesPrevista, Boolean paletizado,
                              String lojaDestino, java.time.LocalDate dataPrevistaEntrega, String origem) {
}
