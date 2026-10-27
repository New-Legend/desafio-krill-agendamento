package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.application.dto.NovoAgendamento;
import br.com.redekrill.agendamento.application.dto.ResultadoAgendamento;
import br.com.redekrill.agendamento.application.dto.Simulacao;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.application.port.FornecedorRepositorio;
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
import br.com.redekrill.agendamento.domain.model.CentroDistribuicao;
import br.com.redekrill.agendamento.domain.model.Fornecedor;
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
    private final CentroDistribuicaoRepositorio cds;
    private final FornecedorRepositorio fornecedores;

    public AgendamentoUseCase(AgendamentoRepositorio agendamentos, FaixaHorariaRepositorio faixas,
                              TipoCaminhaoRepositorio caminhoes, TipoPaletizacaoRepositorio paletizacoes,
                              ParametroRepositorio parametros, NotificadorEmail email, NotificadorPush push,
                              AuditoriaUseCase auditoria, Transacao transacao, Clock clock,
                              GeradorToken tokens, String urlPublica) {
        this(agendamentos, faixas, caminhoes, paletizacoes, parametros, email, push, auditoria, transacao, clock, tokens,
                urlPublica, null, null);
    }

    public AgendamentoUseCase(AgendamentoRepositorio agendamentos, FaixaHorariaRepositorio faixas,
                              TipoCaminhaoRepositorio caminhoes, TipoPaletizacaoRepositorio paletizacoes,
                              ParametroRepositorio parametros, NotificadorEmail email, NotificadorPush push,
                              AuditoriaUseCase auditoria, Transacao transacao, Clock clock,
                              GeradorToken tokens, String urlPublica, CentroDistribuicaoRepositorio cds,
                              FornecedorRepositorio fornecedores) {
        this.cds = cds;
        this.fornecedores = fornecedores;
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
            Long cdId = centroAtivo(cmd.cdId());
            novo.vincular(cdId, fornecedorAtivo(cmd.fornecedorId()));
            OcupacaoDia atual = ocupacaoDoDia(cmd.data(), cdId);
            List<AlertaCapacidade> alertas = calculadora.avaliar(atual, faixa.id(), novo.getPaletesEquivalentes());
            String token = tokens.novo();
            novo.definirTokenFornecedor(tokens.hash(token));
            Agendamento salvo = agendamentos.salvar(novo);
            Map<String, Object> det = new HashMap<>();
            det.put("data", salvo.getData().toString());
            det.put("faixa", faixa.descricao());
            det.put("idPedido", salvo.getIdPedido());
            det.put("fornecedor", salvo.getFornecedor());
            det.put("cd", salvo.getCdId());
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
        return ocupacaoDoDia(data, null);
    }

    /** Ocupação do dia no CD informado (nulo = CD padrão), com as capacidades próprias do CD. */
    public OcupacaoDia ocupacaoDoDia(LocalDate data, Long cdId) {
        return ocupacaoPeriodoSemValidar(data, data, cdId).get(0);
    }

    public List<OcupacaoDia> ocupacaoPeriodo(LocalDate inicio, LocalDate fim) {
        return ocupacaoPeriodo(inicio, fim, null);
    }

    public List<OcupacaoDia> ocupacaoPeriodo(LocalDate inicio, LocalDate fim, Long cdId) {
        if (inicio == null || fim == null) throw new RegraNegocioException("Informe início e fim do período");
        validarPeriodo(inicio, fim);
        return ocupacaoPeriodoSemValidar(inicio, fim, cdId);
    }

    private List<OcupacaoDia> ocupacaoPeriodoSemValidar(LocalDate inicio, LocalDate fim, Long cdId) {
        long cd = cdId == null ? Agendamento.CD_PADRAO : cdId;
        CentroDistribuicao centro = cds == null ? null
                : cds.buscarPorId(cd).orElseThrow(() -> new RecursoNaoEncontradoException("CD", cd));
        int padrao = parametros.obter().capacidadeDiariaPaletes();
        int capacidade = centro == null ? padrao : centro.capacidadeDia(padrao);
        List<FaixaHoraria> ativas = faixas.listar().stream().filter(FaixaHoraria::ativo).toList();
        if (centro != null) ativas = centro.ajustar(ativas);
        List<FaixaHoraria> faixasCd = ativas;
        List<Agendamento> todos = agendamentos.listarPorPeriodo(inicio, fim).stream()
                .filter(a -> a.getCdId() == null || a.getCdId() == cd).toList();
        return inicio.datesUntil(fim.plusDays(1))
                .map(d -> calculadora.calcular(d, capacidade, faixasCd, todos))
                .toList();
    }

    /** Pré-visualiza os alertas de capacidade antes de confirmar (usado na tela de cadastro). */
    public Simulacao simular(LocalDate data, Long faixaId, Long paletizacaoId, int quantidade) {
        return simular(data, faixaId, paletizacaoId, quantidade, null);
    }

    public Simulacao simular(LocalDate data, Long faixaId, Long paletizacaoId, int quantidade, Long cdId) {
        if (data == null) throw new RegraNegocioException("Data é obrigatória");
        OcupacaoDia atual = ocupacaoDoDia(data, cdId);
        int equivalentes = paletizacaoId == null || quantidade <= 0 ? Math.max(quantidade, 0)
                : paletizacao(paletizacaoId).paletesEquivalentes(quantidade);
        return new Simulacao(atual, equivalentes,
                faixaId == null && equivalentes == 0 ? List.of() : calculadora.avaliar(atual, faixaId, equivalentes));
    }

    /**
     * Remarca data/faixa. O CD pode exceder a capacidade (só alerta); no link público o fornecedor
     * só escolhe faixas com vaga ({@code permitirExcesso = false}).
     */
    public ResultadoAgendamento reagendar(Long id, LocalDate data, Long faixaId, Solicitante solicitante,
                                          boolean permitirExcesso) {
        ResultadoAgendamento r = transacao.executar(() -> {
            if (data == null) throw new RegraNegocioException("Data é obrigatória");
            agendamentos.bloquearDia(data);
            Agendamento a = agendamentos.buscarPorId(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
            FaixaHoraria faixa = faixa(faixaId);
            String de = a.getData() + " " + a.getFaixaHorariaId();
            a.reagendar(data, faixa, OffsetDateTime.now(clock));
            OcupacaoDia atual = ocupacaoDoDia(data, a.getCdId());
            List<AlertaCapacidade> alertas = calculadora.avaliar(atual, faixa.id(), a.getPaletesEquivalentes());
            if (!permitirExcesso && !alertas.isEmpty()) {
                throw new RegraNegocioException("A faixa " + faixa.descricao() + " de " + data.format(BR)
                        + " não tem vaga para " + a.getPaletesEquivalentes() + " paletes. Escolha outra.");
            }
            Agendamento salvo = agendamentos.salvar(a);
            auditoria.registrar(solicitante, "REAGENDAR", "AGENDAMENTO", id, Map.of("de", de,
                    "para", data + " " + faixa.id(), "alertas", alertas.stream().map(AlertaCapacidade::mensagem).toList()));
            return new ResultadoAgendamento(detalhar(salvo), alertas, null);
        });
        Agendamento a = r.agendamento().agendamento();
        push.notificarTodos("Agendamento remarcado", a.getFornecedor() + " · " + a.getData().format(BR) + " "
                + r.agendamento().faixaDescricao(), "/agendamentos/" + a.getId());
        if (!r.alertas().isEmpty()) {
            push.notificarTodos("Capacidade excedida", r.alertas().stream().map(AlertaCapacidade::mensagem)
                    .collect(Collectors.joining(" | ")), "/painel?data=" + a.getData());
        }
        return r;
    }

    private Long centroAtivo(Long cdId) {
        if (cdId == null || cds == null) return cdId;
        CentroDistribuicao c = cds.buscarPorId(cdId).orElseThrow(() -> new RecursoNaoEncontradoException("CD", cdId));
        if (!c.ativo()) throw new RegraNegocioException("CD inativo");
        return cdId;
    }

    private Long fornecedorAtivo(Long fornecedorId) {
        if (fornecedorId == null || fornecedores == null) return fornecedorId;
        Fornecedor f = fornecedores.buscarPorId(fornecedorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", fornecedorId));
        if (!f.ativo()) throw new RegraNegocioException("Fornecedor inativo");
        return fornecedorId;
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
