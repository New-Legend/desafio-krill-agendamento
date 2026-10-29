package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tipo de palete configurável (porte de /vale-palete/configuracoes do Gestão CD).
 * O código é a chave usada nas quantidades dos vales e romaneios e não muda depois de criado;
 * o valor de reposição sugere o preço unitário na cobrança de paletes não devolvidos.
 * Pode ser restrito a um CD (cdId) ou valer para todos (nulo).
 */
public record TipoPalete(Long id, String codigo, String nome, BigDecimal valorReposicao, int ordem, boolean ativo, Long cdId) {
    public TipoPalete(Long id, String codigo, String nome, BigDecimal valorReposicao, int ordem, boolean ativo) {
        this(id, codigo, nome, valorReposicao, ordem, ativo, null);
    }

    /** cdId nulo = tipo aceito em todos os CDs. */
    public boolean valeParaCd(Long cd) {
        return cdId == null || cd == null || cdId.equals(cd);
    }

    private static final Pattern CODIGO = Pattern.compile("^[A-Z0-9_]{2,20}$");

    public TipoPalete {
        codigo = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!CODIGO.matcher(codigo).matches()) {
            throw new RegraNegocioException("Código do tipo de palete deve ter de 2 a 20 letras, números ou _");
        }
        nome = Textos.obrigatorio(nome, "Nome do tipo de palete", 40);
        valorReposicao = valorReposicao == null ? BigDecimal.ZERO : valorReposicao;
        if (valorReposicao.signum() < 0) throw new RegraNegocioException("Valor de reposição não pode ser negativo");
        if (ordem < 0) throw new RegraNegocioException("Ordem não pode ser negativa");
    }
}
