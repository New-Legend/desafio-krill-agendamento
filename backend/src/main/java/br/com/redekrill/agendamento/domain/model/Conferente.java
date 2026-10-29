package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

/** Conferente do CD (porte de /recebimento-conferentes). Pausado não aparece ao iniciar a conferência. */
public record Conferente(Long id, Long cdId, String nome, boolean ativo) {
    public Conferente {
        if (cdId == null) throw new RegraNegocioException("CD é obrigatório");
        nome = Textos.obrigatorio(nome, "Nome do conferente", 80).replaceAll("\\s+", " ");
        if (nome.length() < 3 || !nome.contains(" ")) {
            throw new RegraNegocioException("Informe o nome completo do conferente (nome e sobrenome)");
        }
    }
}
