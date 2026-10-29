package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.ConfiguracaoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.util.Map;

/** Configurações do vale-palete (porte de /vale-palete/configuracoes do Gestão CD). */
public class ConfiguracaoValePaleteUseCase {

    public static final String EXIGIR_ASSINATURAS = "vale_palete_exigir_duas_assinaturas";
    public static final String AGRUPAMENTO = "paletes_agrupamento";

    public record Config(boolean exigirDuasAssinaturas, String agrupamento) {
    }

    private final ConfiguracaoRepositorio repo;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;

    public ConfiguracaoValePaleteUseCase(ConfiguracaoRepositorio repo, AuditoriaUseCase auditoria, Transacao transacao) {
        this.repo = repo;
        this.auditoria = auditoria;
        this.transacao = transacao;
    }

    public Config obter() {
        return new Config(exigirDuasAssinaturas(), repo.obter(AGRUPAMENTO).filter("transportadora"::equals).orElse("fornecedor"));
    }

    public boolean exigirDuasAssinaturas() {
        return repo.obter(EXIGIR_ASSINATURAS).map("1"::equals).orElse(false);
    }

    public Config salvar(Config c, Solicitante s) {
        if (c == null || c.agrupamento() == null || !(c.agrupamento().equals("fornecedor") || c.agrupamento().equals("transportadora"))) {
            throw new RegraNegocioException("Agrupamento deve ser fornecedor ou transportadora");
        }
        return transacao.executar(() -> {
            repo.salvar(EXIGIR_ASSINATURAS, c.exigirDuasAssinaturas() ? "1" : "0");
            repo.salvar(AGRUPAMENTO, c.agrupamento());
            auditoria.registrar(s, "ALTERAR", "CONFIGURACAO_VALE_PALETE", null,
                    Map.of("exigirDuasAssinaturas", c.exigirDuasAssinaturas(), "agrupamento", c.agrupamento()));
            return obter();
        });
    }
}
