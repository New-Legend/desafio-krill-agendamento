package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.GeradorToken;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.model.Agendamento;

import java.util.Map;

/**
 * Link público do fornecedor (porte do "link de agendamento" do Gestão CD): o fornecedor acompanha
 * o agendamento, confirma presença e informa placa/motorista sem login, só com o token secreto.
 */
public class FornecedorLinkUseCase {

    private final AgendamentoRepositorio agendamentos;
    private final AgendamentoUseCase agendamentoUseCase;
    private final GeradorToken tokens;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;

    public FornecedorLinkUseCase(AgendamentoRepositorio agendamentos, AgendamentoUseCase agendamentoUseCase,
                                 GeradorToken tokens, AuditoriaUseCase auditoria, Transacao transacao) {
        this.agendamentos = agendamentos;
        this.agendamentoUseCase = agendamentoUseCase;
        this.tokens = tokens;
        this.auditoria = auditoria;
        this.transacao = transacao;
    }

    public AgendamentoDetalhe consultar(String token) {
        return agendamentoUseCase.detalhar(porToken(token));
    }

    public AgendamentoDetalhe confirmar(String token, String ip) {
        return alterar(token, ip, "CONFIRMAR_FORNECEDOR", Agendamento::confirmar);
    }

    public AgendamentoDetalhe informarVeiculo(String token, String placa, String motorista, String ip) {
        return alterar(token, ip, "VEICULO_FORNECEDOR", a -> a.atualizarPeloFornecedor(placa, motorista));
    }

    private AgendamentoDetalhe alterar(String token, String ip, String acao, java.util.function.Consumer<Agendamento> op) {
        return transacao.executar(() -> {
            Agendamento a = porToken(token);
            op.accept(a);
            Agendamento salvo = agendamentos.salvar(a);
            auditoria.registrar(new Solicitante("fornecedor:" + salvo.getEmailFornecedor(), ip), acao, "AGENDAMENTO",
                    salvo.getId(), Map.of("status", salvo.getStatus().name()));
            return agendamentoUseCase.detalhar(salvo);
        });
    }

    private Agendamento porToken(String token) {
        if (token == null || token.length() < 20 || token.length() > 100) {
            throw new RecursoNaoEncontradoException("Link", "inválido");
        }
        return agendamentos.buscarPorTokenHash(tokens.hash(token))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Link", "inválido ou expirado"));
    }
}
