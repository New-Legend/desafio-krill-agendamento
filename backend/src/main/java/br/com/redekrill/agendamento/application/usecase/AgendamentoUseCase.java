package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.application.dto.NovoAgendamento;
import br.com.redekrill.agendamento.application.dto.ResultadoAgendamento;
import br.com.redekrill.agendamento.application.dto.Simulacao;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.FaixaHorariaRepositorio;
import br.com.redekrill.agendamento.application.port.GeradorToken;
import br.com.redekrill.agendamento.application.port.NotificadorEmail;
import br.com.redekrill.agendamento.application.port.NotificadorPush;
import br.com.redekrill.agendamento.application.port.ParametroRepositorio;
import br.com.redekrill.agendamento.application.port.TipoCaminhaoRepositorio;
import br.com.redekrill.agendamento.application.port.TipoPaletizacaoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.AlertaCapacidade;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.OcupacaoDia;
import br.com.redekrill.agendamento.domain.model.TipoCaminhao;
import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;
import br.com.redekrill.agendamento.domain.service.CalculadoraOcupacao;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Casos de uso de agendamento: criar, consultar, acompanhar ocupação e cancelar. */
public class AgendamentoUseCase {

    static final int PERIODO_MAXIMO_DIAS = 366;
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AgendamentoRepositorio agendamentos;
    private final FaixaHorariaRepositorio faixas;
    private final TipoCaminhaoRepositorio caminhoes;
    private final TipoPaletizacaoRepositorio paletizacoes;
    private final ParametroRepositorio parametros;
    private final NotificadorEmail email;
    private final NotificadorPush push;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;
    private final Clock clock;
    private final GeradorToken tokens;
    private final String urlPublica;
    private final CalculadoraOcupacao calculadora = new CalculadoraOcupacao();

    public AgendamentoUseCase(AgendamentoRepositorio agendamentos, FaixaHorariaRepositorio faixas,
                              TipoCaminhaoRepositorio caminhoes, TipoPaletizacaoRepositorio paletizacoes,
                              ParametroRepositorio parametros, NotificadorEmail email, NotificadorPush push,
                              AuditoriaUseCase auditoria, Transacao transacao, Clock clock,
                              GeradorToken tokens, String urlPublica) {
        this.agendamentos = agendamentos;
        this.faixas = faixas;
        this.caminhoes = caminhoes;
        this.paletizacoes = paletizacoes;
        this.parametros = parametros;
        this.email = email;
        this.push = push;
        this.auditoria = auditoria;
        this.transacao = transacao;
        this.clock = clock;
        this.tokens = tokens;
        this.urlPublica = urlPublica.endsWith("/") ? urlPublica.substring(0, urlPublica.length() - 1) : urlPublica;
    }

    public ResultadoAgendamento criar(NovoAgendamento cmd, Solicitante solicitante) {
        if (cmd.data() == null) throw new RegraNegocioException("Data é obrigatória");
        if (cmd.quantidadePaletes() == null) throw new RegraNegocioException("Quantidade de paletes é obrigatória");
        ResultadoAgendamento resultado = transacao.executar(() -> {
            agendamentos.bloquearDia(cmd.data());
            FaixaHoraria faixa = faixa(cmd.faixaHorariaId());
            TipoCaminhao caminhao = caminhao(cmd.tipoCaminhaoId());
            TipoPaletizacao paletizacao = paletizacao(cmd.tipoPaletizacaoId());
            Agendamento novo = Agendamento.novo(cmd.data(), faixa, cmd.idPedido(), cmd.fornecedor(),
                    cmd.emailFornecedor(), caminhao, paletizacao, cmd.quantidadePaletes(), cmd.observacao(),
                    solicitante.usuario(), OffsetDateTime.now(clock));
            OcupacaoDia atual = ocupacaoDoDia(cmd.data());
            List<AlertaCapacidade> alertas = calculadora.avaliar(atual, faixa.id(), novo.getPaletesEquivalentes());
            String token = tokens.novo();
            novo.definirTokenFornecedor(tokens.hash(token));
            Agendamento salvo = agendamentos.salvar(novo);
            Map<String, Object> det = new HashMap<>();
            det.put("data", salvo.getData().toString());
            det.put("faixa", faixa.descricao());
            det.put("idPedido", salvo.getIdPedido());
            det.put("fornecedor", salvo.getFornecedor());
            det.put("paletesEquivalentes", salvo.getPaletesEquivalentes());
            det.put("alertas", alertas.stream().map(AlertaCapacidade::mensagem).toList());
            auditoria.registrar(solicitante, "CRIAR", "AGENDAMENTO", salvo.getId(), det);
            return new ResultadoAgendamento(
                    new AgendamentoDetalhe(salvo, faixa.descricao(), caminhao.nome(), paletizacao.nome()), alertas,
                    linkFornecedor(token));
        });
        Agendamento a = resultado.agendamento().agendamento();
        email.enviarConfirmacao(resultado.agendamento(), resultado.linkFornecedor());
        push.notificarTodos("Novo agendamento",
                a.getFornecedor() + " · " + a.getData().format(BR) + " " + resultado.agendamento().faixaDescricao()
                        + " · " + a.getPaletesEquivalentes() + " paletes", "/agendamentos/" + a.getId());
        if (!resultado.alertas().isEmpty()) {
            push.notificarTodos("Capacidade excedida",
                    resultado.alertas().stream().map(AlertaCapacidade::mensagem).collect(Collectors.joining(" | ")),
                    "/painel?data=" + a.getData());
        }
        return resultado;
    }

    public AgendamentoDetalhe cancelar(Long id, String motivo, Solicitante solicitante) {
        AgendamentoDetalhe detalhe = transacao.executar(() -> {
            Agendamento a = agendamentos.buscarPorId(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
            a.cancelar(solicitante.usuario(), motivo, OffsetDateTime.now(clock));
            Agendamento salvo = agendamentos.salvar(a);
            auditoria.registrar(solicitante, "CANCELAR", "AGENDAMENTO", id,
                    Map.of("motivo", salvo.getMotivoCancelamento(), "data", salvo.getData().toString()));
            return detalhar(salvo);
        });
        Agendamento a = detalhe.agendamento();
        email.enviarCancelamento(detalhe);
        push.notificarTodos("Agendamento cancelado",
                a.getFornecedor() + " · " + a.getData().format(BR) + " " + detalhe.faixaDescricao(),
                "/agendamentos/" + a.getId());
        return detalhe;
    }

    /** Gera um novo link para o fornecedor (o anterior deixa de funcionar). */
    public String gerarNovoLinkFornecedor(Long id, Solicitante solicitante) {
        return transacao.executar(() -> {
            Agendamento a = agendamentos.buscarPorId(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
            String token = tokens.novo();
            a.definirTokenFornecedor(tokens.hash(token));
            agendamentos.salvar(a);
            auditoria.registrar(solicitante, "GERAR_LINK", "AGENDAMENTO", id, Map.of());
            return linkFornecedor(token);
        });
    }

    String linkFornecedor(String token) {
        return urlPublica + "/fornecedor/" + token;
    }

    public AgendamentoDetalhe buscar(Long id) {
        return agendamentos.buscarPorId(id).map(this::detalhar)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
    }

    public List<AgendamentoDetalhe> listar(FiltroAgendamento filtro) {
        validarPeriodo(filtro.inicio(), filtro.fim());
        Map<Long, FaixaHoraria> f = porId(faixas.listar(), FaixaHoraria::id);
        Map<Long, TipoCaminhao> c = porId(caminhoes.listar(), TipoCaminhao::id);
        Map<Long, TipoPaletizacao> p = porId(paletizacoes.listar(), TipoPaletizacao::id);
        return agendamentos.listar(filtro).stream().map(a -> detalhar(a, f, c, p)).toList();
    }

    public OcupacaoDia ocupacaoDoDia(LocalDate data) {
        return calculadora.calcular(data, parametros.obter().capacidadeDiariaPaletes(),
                faixas.listar().stream().filter(FaixaHoraria::ativo).toList(),
                agendamentos.listarPorPeriodo(data, data));
    }

    public List<OcupacaoDia> ocupacaoPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) throw new RegraNegocioException("Informe início e fim do período");
        validarPeriodo(inicio, fim);
        int capacidade = parametros.obter().capacidadeDiariaPaletes();
        List<FaixaHoraria> ativas = faixas.listar().stream().filter(FaixaHoraria::ativo).toList();
        List<Agendamento> todos = agendamentos.listarPorPeriodo(inicio, fim);
        return inicio.datesUntil(fim.plusDays(1))
                .map(d -> calculadora.calcular(d, capacidade, ativas, todos))
                .toList();
    }

    /** Pré-visualiza os alertas de capacidade antes de confirmar (usado na tela de cadastro). */
    public Simulacao simular(LocalDate data, Long faixaId, Long paletizacaoId, int quantidade) {
        if (data == null) throw new RegraNegocioException("Data é obrigatória");
        OcupacaoDia atual = ocupacaoDoDia(data);
        int equivalentes = paletizacaoId == null || quantidade <= 0 ? Math.max(quantidade, 0)
                : paletizacao(paletizacaoId).paletesEquivalentes(quantidade);
        return new Simulacao(atual, equivalentes,
                faixaId == null && equivalentes == 0 ? List.of() : calculadora.avaliar(atual, faixaId, equivalentes));
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null) {
            if (fim.isBefore(inicio)) throw new RegraNegocioException("Data final anterior à data inicial");
            if (ChronoUnit.DAYS.between(inicio, fim) > PERIODO_MAXIMO_DIAS) {
                throw new RegraNegocioException("Período máximo de " + PERIODO_MAXIMO_DIAS + " dias");
            }
        }
    }

    AgendamentoDetalhe detalhar(Agendamento a) {
        return detalhar(a, porId(faixas.listar(), FaixaHoraria::id), porId(caminhoes.listar(), TipoCaminhao::id),
                porId(paletizacoes.listar(), TipoPaletizacao::id));
    }

    private static AgendamentoDetalhe detalhar(Agendamento a, Map<Long, FaixaHoraria> f, Map<Long, TipoCaminhao> c,
                                               Map<Long, TipoPaletizacao> p) {
        FaixaHoraria faixa = f.get(a.getFaixaHorariaId());
        TipoCaminhao cam = c.get(a.getTipoCaminhaoId());
        TipoPaletizacao pal = p.get(a.getTipoPaletizacaoId());
        return new AgendamentoDetalhe(a, faixa == null ? "-" : faixa.descricao(),
                cam == null ? "-" : cam.nome(), pal == null ? "-" : pal.nome());
    }

    private static <T> Map<Long, T> porId(List<T> lista, Function<T, Long> id) {
        return lista.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private FaixaHoraria faixa(Long id) {
        if (id == null) throw new RegraNegocioException("Faixa horária é obrigatória");
        return faixas.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Faixa horária", id));
    }

    private TipoCaminhao caminhao(Long id) {
        if (id == null) throw new RegraNegocioException("Tipo de caminhão é obrigatório");
        return caminhoes.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de caminhão", id));
    }

    private TipoPaletizacao paletizacao(Long id) {
        if (id == null) throw new RegraNegocioException("Tipo de paletização é obrigatório");
        return paletizacoes.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de paletização", id));
    }
}
