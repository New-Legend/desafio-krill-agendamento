package br.com.redekrill.agendamento.infrastructure.config;

import br.com.redekrill.agendamento.application.port.CapacidadeRecebimentoRepositorio;
import br.com.redekrill.agendamento.application.port.ConfiguracaoRepositorio;
import br.com.redekrill.agendamento.application.port.DocaRepositorio;
import br.com.redekrill.agendamento.application.port.ItemAvulsoRepositorio;
import br.com.redekrill.agendamento.application.port.LogisticaRepositorio;
import br.com.redekrill.agendamento.application.port.TaxaDescargaRepositorio;
import br.com.redekrill.agendamento.application.usecase.CapacidadeRecebimentoUseCase;
import br.com.redekrill.agendamento.application.usecase.ConfiguracaoValePaleteUseCase;
import br.com.redekrill.agendamento.application.usecase.ItemAvulsoUseCase;
import br.com.redekrill.agendamento.application.usecase.PatioUseCase;
import br.com.redekrill.agendamento.application.usecase.TaxaDescargaUseCase;
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
import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.application.port.CobrancaPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.FornecedorRepositorio;
import br.com.redekrill.agendamento.application.port.MovimentoPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.NotaFiscalRepositorio;
import br.com.redekrill.agendamento.application.port.ParametroDescargaRepositorio;
import br.com.redekrill.agendamento.application.port.ProdutoRepositorio;
import br.com.redekrill.agendamento.application.usecase.CadastroRecebimentoUseCase;
import br.com.redekrill.agendamento.application.usecase.NotaFiscalUseCase;
import br.com.redekrill.agendamento.application.usecase.PaleteFinanceiroUseCase;
import br.com.redekrill.agendamento.application.usecase.RelatorioRecebimentoUseCase;
import br.com.redekrill.agendamento.application.port.CanalEmailDocumentos;
import br.com.redekrill.agendamento.application.port.ConferenteRepositorio;
import br.com.redekrill.agendamento.application.port.NotificacaoEnvioRepositorio;
import br.com.redekrill.agendamento.application.port.NotificadorWhatsApp;
import br.com.redekrill.agendamento.application.port.TipoPaleteRepositorio;
import br.com.redekrill.agendamento.application.usecase.ConferenteUseCase;
import br.com.redekrill.agendamento.application.usecase.NotificacaoUseCase;
import br.com.redekrill.agendamento.application.usecase.TipoPaleteUseCase;
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
    br.com.redekrill.agendamento.application.usecase.AcessoUseCase acessoUseCase(
            br.com.redekrill.agendamento.application.port.UsuarioRepositorio u,
            br.com.redekrill.agendamento.application.port.PerfilAcessoRepositorio p,
            br.com.redekrill.agendamento.application.port.LogAcessoRepositorio l,
            br.com.redekrill.agendamento.application.port.CodificadorSenha c, AuditoriaUseCase a, Transacao tx, Clock clock) {
        return new br.com.redekrill.agendamento.application.usecase.AcessoUseCase(u, p, l, c, a, tx, clock);
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
                                          @Value("${app.url-publica}") String urlPublica,
                                          CentroDistribuicaoRepositorio cds, FornecedorRepositorio fornecedores) {
        return new AgendamentoUseCase(ag, f, c, t, p, email, push, a, tx, clock, tokens, urlPublica, cds, fornecedores);
    }

    @Bean
    RecebimentoUseCase recebimentoUseCase(AgendamentoRepositorio ag, AgendamentoUseCase uc, AuditoriaUseCase a,
                                          NotificadorPush push, Transacao tx, Clock clock, ConferenteUseCase conf) {
        return new RecebimentoUseCase(ag, uc, a, push, tx, clock, conf);
    }

    @Bean
    FornecedorLinkUseCase fornecedorLinkUseCase(AgendamentoRepositorio ag, AgendamentoUseCase uc, GeradorToken tokens,
                                                AuditoriaUseCase a, Transacao tx, Clock clock) {
        return new FornecedorLinkUseCase(ag, uc, tokens, a, tx, clock);
    }

    @Bean
    ValePaleteUseCase valePaleteUseCase(ValePaleteRepositorio v, AgendamentoRepositorio ag, AuditoriaUseCase a,
                                        Transacao tx, Clock clock, TipoPaleteUseCase tipos,
                                        ConfiguracaoValePaleteUseCase config) {
        return new ValePaleteUseCase(v, ag, a, tx, clock, tipos).comConfiguracao(config);
    }

    @Bean
    DashboardUseCase dashboardUseCase(AgendamentoUseCase uc, Clock clock) {
        return new DashboardUseCase(uc, clock);
    }

    @Bean
    PedidoCompraUseCase pedidoCompraUseCase(PedidoCompraGateway g, TipoPaletizacaoRepositorio t) {
        return new PedidoCompraUseCase(g, t);
    }

    @Bean
    CadastroRecebimentoUseCase cadastroRecebimentoUseCase(CentroDistribuicaoRepositorio cds, FornecedorRepositorio f,
                                                          ProdutoRepositorio p, ParametroDescargaRepositorio d,
                                                          TipoCaminhaoRepositorio c, AuditoriaUseCase a, Transacao tx) {
        return new CadastroRecebimentoUseCase(cds, f, p, d, c, a, tx);
    }

    @Bean
    NotaFiscalUseCase notaFiscalUseCase(NotaFiscalRepositorio n, AgendamentoRepositorio ag, ProdutoRepositorio p,
                                        FornecedorRepositorio f, AuditoriaUseCase a, Transacao tx, Clock clock) {
        return new NotaFiscalUseCase(n, ag, p, f, a, tx, clock);
    }

    @Bean
    RelatorioRecebimentoUseCase relatorioRecebimentoUseCase(AgendamentoUseCase uc, FaixaHorariaRepositorio f,
                                                            ParametroDescargaRepositorio d, NotaFiscalRepositorio n,
                                                            ValePaleteRepositorio v, CobrancaPaleteRepositorio c, Clock clock) {
        return new RelatorioRecebimentoUseCase(uc, f, d, n, v, c, clock);
    }

    @Bean
    PaleteFinanceiroUseCase paleteFinanceiroUseCase(ValePaleteRepositorio v, CobrancaPaleteRepositorio c,
                                                    MovimentoPaleteRepositorio m, CentroDistribuicaoRepositorio cds,
                                                    AuditoriaUseCase a, Transacao tx, Clock clock, TipoPaleteUseCase tipos) {
        return new PaleteFinanceiroUseCase(v, c, m, cds, a, tx, clock, tipos);
    }

    @Bean
    TipoPaleteUseCase tipoPaleteUseCase(TipoPaleteRepositorio r, AuditoriaUseCase a, Transacao tx) {
        return new TipoPaleteUseCase(r, a, tx);
    }

    @Bean
    ConferenteUseCase conferenteUseCase(ConferenteRepositorio r, CentroDistribuicaoRepositorio cds, AuditoriaUseCase a, Transacao tx) {
        return new ConferenteUseCase(r, cds, a, tx);
    }

    @Bean
    NotificacaoUseCase notificacaoUseCase(PaleteFinanceiroUseCase p, AgendamentoUseCase ag, FornecedorRepositorio f,
                                          CanalEmailDocumentos email, NotificadorWhatsApp w, NotificacaoEnvioRepositorio e,
                                          AuditoriaUseCase a, Clock clock) {
        return new NotificacaoUseCase(p, ag, f, email, w, e, a, clock);
    }

    @Bean
    ConfiguracaoValePaleteUseCase configuracaoValePaleteUseCase(ConfiguracaoRepositorio r, AuditoriaUseCase a, Transacao tx) {
        return new ConfiguracaoValePaleteUseCase(r, a, tx);
    }

    @Bean
    CapacidadeRecebimentoUseCase capacidadeRecebimentoUseCase(CapacidadeRecebimentoRepositorio r, LogisticaRepositorio l,
                                                              CentroDistribuicaoRepositorio cds, ParametroRepositorio p,
                                                              AuditoriaUseCase a, Transacao tx) {
        return new CapacidadeRecebimentoUseCase(r, l, cds, p, a, tx);
    }

    @Bean
    TaxaDescargaUseCase taxaDescargaUseCase(TaxaDescargaRepositorio r, AuditoriaUseCase a, Transacao tx) {
        return new TaxaDescargaUseCase(r, a, tx);
    }

    @Bean
    PatioUseCase patioUseCase(DocaRepositorio d, LogisticaRepositorio l, AgendamentoRepositorio ag, AgendamentoUseCase uc,
                              CentroDistribuicaoRepositorio cds, CapacidadeRecebimentoUseCase cap, TaxaDescargaUseCase t,
                              AuditoriaUseCase a, Transacao tx, Clock clock) {
        return new PatioUseCase(d, l, ag, uc, cds, cap, t, a, tx, clock);
    }

    @Bean
    ItemAvulsoUseCase itemAvulsoUseCase(ItemAvulsoRepositorio r, ProdutoRepositorio p, AgendamentoRepositorio ag,
                                        AuditoriaUseCase a, Transacao tx, Clock clock) {
        return new ItemAvulsoUseCase(r, p, ag, a, tx, clock);
    }
}
