package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.util.Locale;

/** Doca de descarga de um CD (porte de /yms/docas do Gestão CD). */
public record Doca(Long id, Long cdId, String codigo, String nome, boolean ativa) {
    public Doca {
        if (cdId == null) throw new RegraNegocioException("CD é obrigatório");
        codigo = Textos.obrigatorio(codigo, "Código da doca", 10).toUpperCase(Locale.ROOT);
        nome = Textos.obrigatorio(nome, "Nome da doca", 40);
    }
}
