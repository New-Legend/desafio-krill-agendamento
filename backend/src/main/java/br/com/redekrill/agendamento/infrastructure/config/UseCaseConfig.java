package br.com.redekrill.agendamento.infrastructure.config;

import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.AuditoriaRepositorio;
import br.com.redekrill.agendamento.application.port.FaixaHorariaRepositorio;
import br.com.redekrill.agendamento.application.port.NotificadorEmail;
import br.com.redekrill.agendamento.application.port.NotificadorPush;
import br.com.redekrill.agendamento.application.port.ParametroRepositorio;
import br.com.redekrill.agendamento.application.port.TipoCaminhaoRepositorio;
import br.com.redekrill.agendamento.application.port.TipoPaletizacaoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.application.usecase.AgendamentoUseCase;
import br.com.redekrill.agendamento.application.usecase.AuditoriaUseCase;
import br.com.redekrill.agendamento.application.usecase.CadastroUseCase;
import br.com.redekrill.agendamento.application.usecase.DashboardUseCase;
import br.com.redekrill.agendamento.application.usecase.FornecedorLinkUseCase;
import br.com.redekrill.agendamento.application.usecase.PedidoCompraUseCase;
import br.com.redekrill.agendamento.application.usecase.RecebimentoUseCase;
import br.com.redekrill.agendamento.application.usecase.ValePaleteUseCase;
import br.com.redekrill.agendamento.application.port.GeradorToken;
import br.com.redekrill.agendamento.application.port.PedidoCompraGateway;
import br.com.redekrill.agendamento.application.port.ValePaleteRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Monta os casos de uso (camada de aplicação sem dependência de Spring). */
@Configuration
public class UseCaseConfig {

    @Bean
    Clock clock(@Value("${app.timezone}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    @Bean
    AuditoriaUseCase auditoriaUseCase(AuditoriaRepositorio repo, Clock clock) {
        return new AuditoriaUseCase(repo, clock);
    }

    @Bean
    CadastroUseCase cadastroUseCase(ParametroRepositorio p, FaixaHorariaRepositorio f, TipoCaminhaoRepositorio c,
                                    TipoPaletizacaoRepositorio t, AuditoriaUseCase a, Transacao tx) {
        return new CadastroUseCase(p, f, c, t, a, tx);
    }

    @Bean
    AgendamentoUseCase agendamentoUseCase(AgendamentoRepositorio ag, FaixaHorariaRepositorio f,
                                          TipoCaminhaoRepositorio c, TipoPaletizacaoRepositorio t,
                                          ParametroRepositorio p, NotificadorEmail email, NotificadorPush push,
                                          AuditoriaUseCase a, Transacao tx, Clock clock, GeradorToken tokens,
                                          @Value("${app.url-publica}") String urlPublica) {
        return new AgendamentoUseCase(ag, f, c, t, p, email, push, a, tx, clock, tokens, urlPublica);
    }

    @Bean
    RecebimentoUseCase recebimentoUseCase(AgendamentoRepositorio ag, AgendamentoUseCase uc, AuditoriaUseCase a,
                                          NotificadorPush push, Transacao tx, Clock clock) {
        return new RecebimentoUseCase(ag, uc, a, push, tx, clock);
    }

    @Bean
    FornecedorLinkUseCase fornecedorLinkUseCase(AgendamentoRepositorio ag, AgendamentoUseCase uc, GeradorToken tokens,
                                                AuditoriaUseCase a, Transacao tx) {
        return new FornecedorLinkUseCase(ag, uc, tokens, a, tx);
    }

    @Bean
    ValePaleteUseCase valePaleteUseCase(ValePaleteRepositorio v, AgendamentoRepositorio ag, AuditoriaUseCase a,
                                        Transacao tx, Clock clock) {
        return new ValePaleteUseCase(v, ag, a, tx, clock);
    }

    @Bean
    DashboardUseCase dashboardUseCase(AgendamentoUseCase uc, Clock clock) {
        return new DashboardUseCase(uc, clock);
    }

    @Bean
    PedidoCompraUseCase pedidoCompraUseCase(PedidoCompraGateway g, TipoPaletizacaoRepositorio t) {
        return new PedidoCompraUseCase(g, t);
    }
}
