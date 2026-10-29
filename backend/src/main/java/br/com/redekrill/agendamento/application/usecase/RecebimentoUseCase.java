package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.NotificadorPush;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Fluxo operacional do recebimento (porte da Portaria/Recebimento operacional do Gestão CD):
 * confirmar → chegada na portaria → início da conferência → conclusão (com divergência) ou não comparecimento.
 */
public class RecebimentoUseCase {

    public enum Acao { CONFIRMAR, CHEGADA, INICIAR, CONCLUIR, NAO_COMPARECEU }

    public record Comando(Acao acao, String placa, String motorista, String conferente, Integer paletesRecebidos,
                          String divergencia, String motivo) {
    }

    private final AgendamentoRepositorio agendamentos;
    private final AgendamentoUseCase agendamentoUseCase;
    private final AuditoriaUseCase auditoria;
    private final NotificadorPush push;
    private final Transacao transacao;
    private final Clock clock;
    private final ConferenteUseCase conferentes;

    public RecebimentoUseCase(AgendamentoRepositorio agendamentos, AgendamentoUseCase agendamentoUseCase,
                              AuditoriaUseCase auditoria, NotificadorPush push, Transacao transacao, Clock clock) {
        this(agendamentos, agendamentoUseCase, auditoria, push, transacao, clock, null);
    }

    public RecebimentoUseCase(AgendamentoRepositorio agendamentos, AgendamentoUseCase agendamentoUseCase,
                              AuditoriaUseCase auditoria, NotificadorPush push, Transacao transacao, Clock clock,
                              ConferenteUseCase conferentes) {
        this.conferentes = conferentes;
        this.agendamentos = agendamentos;
        this.agendamentoUseCase = agendamentoUseCase;
        this.auditoria = auditoria;
        this.push = push;
        this.transacao = transacao;
        this.clock = clock;
    }

    public AgendamentoDetalhe executar(Long id, Comando cmd, Solicitante solicitante) {
        if (cmd == null || cmd.acao() == null) throw new RegraNegocioException("Informe a ação");
        OffsetDateTime agora = OffsetDateTime.now(clock);
        Consumer<Agendamento> op = switch (cmd.acao()) {
            case CONFIRMAR -> Agendamento::confirmar;
            case CHEGADA -> a -> a.registrarChegada(cmd.placa(), cmd.motorista(), agora);
            case INICIAR -> a -> a.iniciarRecebimento(conferentes == null ? cmd.conferente()
                    : conferentes.validarParaConferencia(a.getCdId(), cmd.conferente()), agora);
            case CONCLUIR -> a -> a.concluirRecebimento(cmd.paletesRecebidos(), cmd.divergencia(), agora);
            case NAO_COMPARECEU -> a -> a.marcarNaoCompareceu(solicitante.usuario(), cmd.motivo(), agora);
        };
        AgendamentoDetalhe d = transacao.executar(() -> {
            Agendamento a = agendamentos.buscarPorId(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
            String antes = a.getStatus().name();
            op.accept(a);
            Agendamento salvo = agendamentos.salvar(a);
            Map<String, Object> det = new HashMap<>();
            det.put("de", antes);
            det.put("para", salvo.getStatus().name());
            if (salvo.getPlaca() != null) det.put("placa", salvo.getPlaca());
            if (cmd.acao() == Acao.CONCLUIR) {
                det.put("paletesRecebidos", salvo.getPaletesRecebidos());
                if (salvo.divergente()) det.put("divergencia", salvo.getDivergencia());
            }
            auditoria.registrar(solicitante, cmd.acao().name(), "AGENDAMENTO", id, det);
            return agendamentoUseCase.detalhar(salvo);
        });
        Agendamento a = d.agendamento();
        if (cmd.acao() == Acao.CHEGADA) {
            push.notificarTodos("Caminhão na portaria", a.getFornecedor() + " · placa " + a.getPlaca(), "/agendamentos/" + id);
        } else if (cmd.acao() == Acao.CONCLUIR && a.divergente()) {
            push.notificarTodos("Recebimento com divergência", a.getFornecedor() + " · " + a.getDivergencia(), "/agendamentos/" + id);
        }
        return d;
    }
}
