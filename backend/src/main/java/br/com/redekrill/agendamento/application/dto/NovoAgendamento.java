package br.com.redekrill.agendamento.application.dto;

import java.time.LocalDate;

public record NovoAgendamento(LocalDate data, Long faixaHorariaId, Long idPedido, String fornecedor,
                              String emailFornecedor, Long tipoCaminhaoId, Long tipoPaletizacaoId,
                              Integer quantidadePaletes, String observacao, Long cdId, Long fornecedorId) {
    public NovoAgendamento(LocalDate data, Long faixaHorariaId, Long idPedido, String fornecedor, String emailFornecedor,
                           Long tipoCaminhaoId, Long tipoPaletizacaoId, Integer quantidadePaletes, String observacao) {
        this(data, faixaHorariaId, idPedido, fornecedor, emailFornecedor, tipoCaminhaoId, tipoPaletizacaoId,
                quantidadePaletes, observacao, null, null);
    }
}
