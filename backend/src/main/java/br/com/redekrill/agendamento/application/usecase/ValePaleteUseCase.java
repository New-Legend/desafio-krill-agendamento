package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.application.port.ValePaleteRepositorio;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.ValePalete;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Vale-palete: emissão, assinaturas (motorista + conferente), devolução e cancelamento. */
public class ValePaleteUseCase {

    public record NovoVale(Long agendamentoId, LocalDate data, String fornecedor, String placa, String motorista,
                           ValePalete.Quantidades quantidades, String observacao) {
    }

    private final ValePaleteRepositorio vales;
    private final AgendamentoRepositorio agendamentos;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;
    private final Clock clock;
    private final TipoPaleteUseCase tipos;

    public ValePaleteUseCase(ValePaleteRepositorio vales, AgendamentoRepositorio agendamentos, AuditoriaUseCase auditoria,
                             Transacao transacao, Clock clock) {
        this(vales, agendamentos, auditoria, transacao, clock, null);
    }

    public ValePaleteUseCase(ValePaleteRepositorio vales, AgendamentoRepositorio agendamentos, AuditoriaUseCase auditoria,
                             Transacao transacao, Clock clock, TipoPaleteUseCase tipos) {
        this.tipos = tipos;
        this.vales = vales;
        this.agendamentos = agendamentos;
        this.auditoria = auditoria;
        this.transacao = transacao;
        this.clock = clock;
    }

    public ValePalete emitir(NovoVale cmd, Solicitante solicitante) {
        return transacao.executar(() -> {
            if (tipos != null && cmd.quantidades() != null) tipos.validarNovo(cmd.quantidades());
            OffsetDateTime agora = OffsetDateTime.now(clock);
            LocalDate data = cmd.data() == null ? agora.toLocalDate() : cmd.data();
            String fornecedor = cmd.fornecedor();
            String placa = cmd.placa();
            String motorista = cmd.motorista();
            if (cmd.agendamentoId() != null) {
                Agendamento a = agendamentos.buscarPorId(cmd.agendamentoId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", cmd.agendamentoId()));
                if (fornecedor == null || fornecedor.isBlank()) fornecedor = a.getFornecedor();
                if (placa == null || placa.isBlank()) placa = a.getPlaca();
                if (motorista == null || motorista.isBlank()) motorista = a.getMotorista();
            }
            ValePalete v = new ValePalete(null, ValePalete.gerarCodigo(data, vales.proximaSequencia()), cmd.agendamentoId(),
                    data, fornecedor, placa, motorista, cmd.quantidades(), ValePalete.Quantidades.ZERO,
                    ValePalete.Status.PENDENTE, cmd.observacao(), null, null, solicitante.usuario(), agora);
            ValePalete salvo = vales.salvar(v);
            auditoria.registrar(solicitante, "CRIAR", "VALE_PALETE", salvo.getId(),
                    Map.of("codigo", salvo.getCodigo(), "fornecedor", salvo.getFornecedor(), "paletes", salvo.getEntregues().total()));
            return salvo;
        });
    }

    public List<ValePalete> listar(LocalDate inicio, LocalDate fim, ValePalete.Status status, String fornecedor) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) throw new RegraNegocioException("Data final anterior à data inicial");
        return vales.listar(inicio, fim, status, fornecedor);
    }

    public ValePalete buscar(Long id) {
        return vales.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Vale-palete", id));
    }

    public ValePalete buscarPorCodigo(String codigo) {
        return vales.buscarPorCodigo(codigo == null ? "" : codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vale-palete", codigo));
    }

    public ValePalete assinar(Long id, ValePalete.Assinante quem, String nome, String imagem, Solicitante s) {
        return alterar(id, s, "ASSINAR_" + quem.name(), v -> v.assinar(quem, nome, imagem, OffsetDateTime.now(clock)));
    }

    public ValePalete refazerAssinatura(Long id, ValePalete.Assinante quem, Solicitante s) {
        return alterar(id, s, "REFAZER_ASSINATURA_" + quem.name(), v -> v.removerAssinatura(quem));
    }

    public ValePalete registrarDevolucao(Long id, ValePalete.Quantidades q, Solicitante s) {
        return alterar(id, s, "DEVOLUCAO", v -> v.registrarDevolucao(q));
    }

    public ValePalete cancelar(Long id, Solicitante s) {
        return alterar(id, s, "CANCELAR", ValePalete::cancelar);
    }

    private ValePalete alterar(Long id, Solicitante s, String acao, java.util.function.Consumer<ValePalete> op) {
        return transacao.executar(() -> {
            ValePalete v = buscar(id);
            op.accept(v);
            ValePalete salvo = vales.salvar(v);
            auditoria.registrar(s, acao, "VALE_PALETE", id, Map.of("codigo", salvo.getCodigo(), "status", salvo.getStatus().name(),
                    "saldo", salvo.saldo().total()));
            return salvo;
        });
    }
}
