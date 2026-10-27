package br.com.redekrill.agendamento.web.dto;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.AlertaCapacidade;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.OcupacaoDia;
import br.com.redekrill.agendamento.domain.model.OcupacaoFaixa;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public final class Respostas {
    private Respostas() {
    }

    public record AgendamentoResp(Long id, LocalDate data, Long faixaHorariaId, String faixaHoraria, Long idPedido,
                                  String fornecedor, String emailFornecedor, Long tipoCaminhaoId, String tipoCaminhao,
                                  Long tipoPaletizacaoId, String tipoPaletizacao, int quantidadePaletes,
                                  int paletesEquivalentes, String observacao, StatusAgendamento status,
                                  String criadoPor, OffsetDateTime criadoEm, String canceladoPor,
                                  OffsetDateTime canceladoEm, String motivoCancelamento, String placa, String motorista,
                                  OffsetDateTime chegadaEm, String conferente, OffsetDateTime inicioRecebimentoEm,
                                  OffsetDateTime fimRecebimentoEm, Integer paletesRecebidos, String divergencia,
                                  String statusDescricao, Long cdId, Long fornecedorId) {
        public static AgendamentoResp de(AgendamentoDetalhe d) {
            Agendamento a = d.agendamento();
            return new AgendamentoResp(a.getId(), a.getData(), a.getFaixaHorariaId(), d.faixaDescricao(),
                    a.getIdPedido(), a.getFornecedor(), a.getEmailFornecedor(), a.getTipoCaminhaoId(),
                    d.tipoCaminhao(), a.getTipoPaletizacaoId(), d.tipoPaletizacao(), a.getQuantidadePaletes(),
                    a.getPaletesEquivalentes(), a.getObservacao(), a.getStatus(), a.getCriadoPor(),
                    a.getCriadoEm(), a.getCanceladoPor(), a.getCanceladoEm(), a.getMotivoCancelamento(), a.getPlaca(),
                    a.getMotorista(), a.getChegadaEm(), a.getConferente(), a.getInicioRecebimentoEm(),
                    a.getFimRecebimentoEm(), a.getPaletesRecebidos(), a.getDivergencia(), a.getStatus().rotulo(),
                    a.getCdId(), a.getFornecedorId());
        }
    }

    public record CriacaoResp(AgendamentoResp agendamento, List<AlertaCapacidade> alertas, String linkFornecedor) {
    }

    public record OcupacaoFaixaResp(Long faixaHorariaId, String descricao, LocalTime horaInicio, LocalTime horaFim,
                                    int capacidade, int paletesOcupados, double percentual, int excedente) {
        static OcupacaoFaixaResp de(OcupacaoFaixa o) {
            FaixaHoraria f = o.faixa();
            return new OcupacaoFaixaResp(f.id(), f.descricao(), f.horaInicio(), f.horaFim(), o.capacidade(),
                    o.paletesOcupados(), o.percentual(), o.excedente());
        }
    }

    public record OcupacaoDiaResp(LocalDate data, int capacidade, int paletesOcupados, double percentual,
                                  int excedente, int quantidadeAgendamentos, List<OcupacaoFaixaResp> faixas) {
        public static OcupacaoDiaResp de(OcupacaoDia o) {
            return new OcupacaoDiaResp(o.data(), o.capacidade(), o.paletesOcupados(), o.percentual(), o.excedente(),
                    o.quantidadeAgendamentos(), o.faixas().stream().map(OcupacaoFaixaResp::de).toList());
        }
    }

    public record SimulacaoResp(OcupacaoDiaResp ocupacaoAtual, int paletesEquivalentes, List<AlertaCapacidade> alertas) {
    }

    public record FaixaResp(Long id, String descricao, LocalTime horaInicio, LocalTime horaFim, int capacidadePaletes,
                            boolean ativo) {
        public static FaixaResp de(FaixaHoraria f) {
            return new FaixaResp(f.id(), f.descricao(), f.horaInicio(), f.horaFim(), f.capacidadePaletes(), f.ativo());
        }
    }

    /** Visão pública (link do fornecedor): sem dados internos de auditoria. */
    public record AgendamentoPublicoResp(Long id, LocalDate data, String faixaHoraria, Long idPedido, String fornecedor,
                                         String tipoCaminhao, String tipoPaletizacao, int quantidadePaletes,
                                         StatusAgendamento status, String statusDescricao, String placa, String motorista,
                                         boolean editavel, Long faixaHorariaId, int paletesEquivalentes) {
        public static AgendamentoPublicoResp de(AgendamentoDetalhe d) {
            Agendamento a = d.agendamento();
            return new AgendamentoPublicoResp(a.getId(), a.getData(), d.faixaDescricao(), a.getIdPedido(), a.getFornecedor(),
                    d.tipoCaminhao(), d.tipoPaletizacao(), a.getQuantidadePaletes(), a.getStatus(), a.getStatus().rotulo(),
                    a.getPlaca(), a.getMotorista(), a.getStatus().antesDaChegada(), a.getFaixaHorariaId(),
                    a.getPaletesEquivalentes());
        }
    }

    public record ValePaleteResp(Long id, String codigo, Long agendamentoId, LocalDate data, String fornecedor, String placa,
                                 String motorista, br.com.redekrill.agendamento.domain.model.ValePalete.Quantidades entregues,
                                 br.com.redekrill.agendamento.domain.model.ValePalete.Quantidades devolvidos,
                                 br.com.redekrill.agendamento.domain.model.ValePalete.Quantidades saldo, String status,
                                 String observacao, br.com.redekrill.agendamento.domain.model.ValePalete.Assinatura assinaturaMotorista,
                                 br.com.redekrill.agendamento.domain.model.ValePalete.Assinatura assinaturaConferente,
                                 String criadoPor, OffsetDateTime criadoEm) {
        public static ValePaleteResp de(br.com.redekrill.agendamento.domain.model.ValePalete v) {
            return new ValePaleteResp(v.getId(), v.getCodigo(), v.getAgendamentoId(), v.getData(), v.getFornecedor(), v.getPlaca(),
                    v.getMotorista(), v.getEntregues(), v.getDevolvidos(), v.saldo(), v.getStatus().name(), v.getObservacao(),
                    v.getAssinaturaMotorista(), v.getAssinaturaConferente(), v.getCriadoPor(), v.getCriadoEm());
        }
    }

    public record Erro(int status, String mensagem, List<String> detalhes, OffsetDateTime dataHora) {
    }
}
