package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.FiltroAuditoria;
import br.com.redekrill.agendamento.application.dto.Pagina;
import br.com.redekrill.agendamento.application.dto.RegistroAuditoria;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AuditoriaRepositorio;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

public class AuditoriaUseCase {

    private static final int TAMANHO_MAXIMO_PAGINA = 200;

    private final AuditoriaRepositorio repositorio;
    private final Clock clock;

    public AuditoriaUseCase(AuditoriaRepositorio repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    public void registrar(Solicitante solicitante, String acao, String entidade, Object entidadeId,
                          boolean sucesso, Map<String, Object> detalhes) {
        repositorio.registrar(new RegistroAuditoria(null, OffsetDateTime.now(clock),
                solicitante == null ? "anonimo" : solicitante.usuario(), acao, entidade,
                entidadeId == null ? null : String.valueOf(entidadeId),
                solicitante == null ? null : solicitante.ip(), sucesso,
                detalhes == null ? Map.of() : detalhes));
    }

    public void registrar(Solicitante solicitante, String acao, String entidade, Object entidadeId,
                          Map<String, Object> detalhes) {
        registrar(solicitante, acao, entidade, entidadeId, true, detalhes);
    }

    public Pagina<RegistroAuditoria> listar(FiltroAuditoria filtro, int pagina, int tamanho) {
        int tam = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_PAGINA);
        return repositorio.listar(filtro, Math.max(pagina, 0), tam);
    }
}
