package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.domain.model.OcupacaoDia;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Home/dashboard: ocupação de hoje, próximos caminhões, contagem por status e alertas. */
public class DashboardUseCase {

    public record Alerta(String tipo, String nivel, LocalDate data, String mensagem, Long agendamentoId) {
    }

    public record Resumo(LocalDate data, OcupacaoDia ocupacao, Map<StatusAgendamento, Long> porStatus,
                         List<AgendamentoDetalhe> proximos, List<Alerta> alertas, List<OcupacaoDia> proximosDias) {
    }

    static final int DIAS_ALERTA = 7;
    static final int LIMITE_PROXIMOS = 8;

    private final AgendamentoUseCase agendamentos;
    private final Clock clock;

    public DashboardUseCase(AgendamentoUseCase agendamentos, Clock clock) {
        this.agendamentos = agendamentos;
        this.clock = clock;
    }

    public Resumo resumo(LocalDate dataInformada) {
        OffsetDateTime agora = OffsetDateTime.now(clock);
        LocalDate hoje = agora.toLocalDate();
        LocalDate data = dataInformada == null ? hoje : dataInformada;
        OcupacaoDia ocupacao = agendamentos.ocupacaoDoDia(data);

        List<AgendamentoDetalhe> doDia = agendamentos.listar(new FiltroAgendamento(data, data, null, null, null, null));
        Map<StatusAgendamento, Long> porStatus = new EnumMap<>(StatusAgendamento.class);
        for (StatusAgendamento s : StatusAgendamento.values()) porStatus.put(s, 0L);
        doDia.forEach(d -> porStatus.merge(d.agendamento().getStatus(), 1L, Long::sum));

        LocalTime horaAgora = data.equals(hoje) ? agora.toLocalTime() : LocalTime.MIN;
        List<AgendamentoDetalhe> proximos = agendamentos.listar(new FiltroAgendamento(data, data.plusDays(DIAS_ALERTA), null, null, null, null))
                .stream()
                .filter(d -> d.agendamento().getStatus().antesDaChegada() || d.agendamento().getStatus() == StatusAgendamento.CHEGOU)
                .filter(d -> d.agendamento().getData().isAfter(data) || !fimDaFaixa(d).isBefore(horaAgora))
                .sorted(Comparator.comparing((AgendamentoDetalhe d) -> d.agendamento().getData()).thenComparing(AgendamentoDetalhe::faixaDescricao))
                .limit(LIMITE_PROXIMOS)
                .toList();

        List<OcupacaoDia> dias = agendamentos.ocupacaoPeriodo(data, data.plusDays(DIAS_ALERTA - 1));
        List<Alerta> alertas = new ArrayList<>();
        for (OcupacaoDia d : dias) {
            if (d.excedente() > 0) {
                alertas.add(new Alerta("CAPACIDADE_DIA", "erro", d.data(),
                        "Capacidade do dia excedida em " + d.excedente() + " paletes (" + d.paletesOcupados() + "/" + d.capacidade() + ")", null));
            }
            d.faixas().stream().filter(f -> f.excedente() > 0).forEach(f -> alertas.add(new Alerta("CAPACIDADE_FAIXA", "aviso", d.data(),
                    "Faixa " + f.faixa().descricao() + " excedida em " + f.excedente() + " paletes", null)));
        }
        agendamentos.listar(new FiltroAgendamento(data.minusDays(1), data.plusDays(DIAS_ALERTA), StatusAgendamento.CANCELADO, null, null, null))
                .stream()
                .filter(d -> d.agendamento().getCanceladoEm() != null && d.agendamento().getCanceladoEm().isAfter(agora.minusHours(24)))
                .forEach(d -> alertas.add(new Alerta("CANCELAMENTO", "info", d.agendamento().getData(),
                        "Cancelado: " + d.agendamento().getFornecedor() + " (pedido " + d.agendamento().getIdPedido() + ")",
                        d.agendamento().getId())));
        doDia.stream().filter(d -> d.agendamento().divergente()).forEach(d -> alertas.add(new Alerta("DIVERGENCIA", "aviso", data,
                "Divergência: " + d.agendamento().getFornecedor() + " · " + d.agendamento().getDivergencia(), d.agendamento().getId())));
        return new Resumo(data, ocupacao, porStatus, proximos, alertas, dias);
    }

    private static LocalTime fimDaFaixa(AgendamentoDetalhe d) {
        String desc = d.faixaDescricao();
        try {
            return LocalTime.parse(desc.substring(desc.length() - 5));
        } catch (RuntimeException e) {
            return LocalTime.MAX;
        }
    }
}
