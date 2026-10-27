package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.FaixaHorariaRepositorio;
import br.com.redekrill.agendamento.application.port.ParametroRepositorio;
import br.com.redekrill.agendamento.application.port.TipoCaminhaoRepositorio;
import br.com.redekrill.agendamento.application.port.TipoPaletizacaoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.ParametroCapacidade;
import br.com.redekrill.agendamento.domain.model.TipoCaminhao;
import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;
import br.com.redekrill.agendamento.domain.service.ValidadorFaixas;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Cadastros de parâmetros de capacidade, faixas horárias, tipos de caminhão e de paletização. */
public class CadastroUseCase {

    private final ParametroRepositorio parametros;
    private final FaixaHorariaRepositorio faixas;
    private final TipoCaminhaoRepositorio caminhoes;
    private final TipoPaletizacaoRepositorio paletizacoes;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;
    private final ValidadorFaixas validadorFaixas = new ValidadorFaixas();

    public CadastroUseCase(ParametroRepositorio parametros, FaixaHorariaRepositorio faixas,
                           TipoCaminhaoRepositorio caminhoes, TipoPaletizacaoRepositorio paletizacoes,
                           AuditoriaUseCase auditoria, Transacao transacao) {
        this.parametros = parametros;
        this.faixas = faixas;
        this.caminhoes = caminhoes;
        this.paletizacoes = paletizacoes;
        this.auditoria = auditoria;
        this.transacao = transacao;
    }

    // ---- Parâmetros de capacidade
    public ParametroCapacidade obterParametro() {
        return parametros.obter();
    }

    public ParametroCapacidade atualizarParametro(ParametroCapacidade novo, Solicitante solicitante) {
        return transacao.executar(() -> {
            ParametroCapacidade anterior = parametros.obter();
            long ativas = faixas.listar().stream().filter(FaixaHoraria::ativo).count();
            if (ativas > novo.quantidadeFaixas()) {
                throw new RegraNegocioException("Existem " + ativas + " faixas ativas; inative faixas antes de reduzir a quantidade");
            }
            ParametroCapacidade salvo = parametros.salvar(novo);
            auditoria.registrar(solicitante, "ALTERAR", "PARAMETRO_CAPACIDADE", 1,
                    Map.of("anterior", anterior.toString(), "novo", salvo.toString()));
            return salvo;
        });
    }

    // ---- Faixas horárias
    public List<FaixaHoraria> listarFaixas() {
        return faixas.listar().stream().sorted(Comparator.comparing(FaixaHoraria::horaInicio)).toList();
    }

    public FaixaHoraria salvarFaixa(FaixaHoraria faixa, Solicitante solicitante) {
        return transacao.executar(() -> {
            if (faixa.id() != null) {
                faixas.buscarPorId(faixa.id()).orElseThrow(() -> new RecursoNaoEncontradoException("Faixa horária", faixa.id()));
            }
            validadorFaixas.validar(faixa, faixas.listar(), parametros.obter());
            FaixaHoraria salva = faixas.salvar(faixa);
            auditoria.registrar(solicitante, faixa.id() == null ? "CRIAR" : "ALTERAR", "FAIXA_HORARIA", salva.id(),
                    Map.of("faixa", salva.descricao(), "capacidade", salva.capacidadePaletes(), "ativo", salva.ativo()));
            return salva;
        });
    }

    // ---- Tipos de caminhão
    public List<TipoCaminhao> listarTiposCaminhao() {
        return caminhoes.listar().stream().sorted(Comparator.comparing(TipoCaminhao::nome)).toList();
    }

    public TipoCaminhao salvarTipoCaminhao(TipoCaminhao tipo, Solicitante solicitante) {
        return transacao.executar(() -> {
            if (tipo.id() != null) {
                caminhoes.buscarPorId(tipo.id()).orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de caminhão", tipo.id()));
            }
            if (caminhoes.existeNome(tipo.nome(), tipo.id())) {
                throw new RegraNegocioException("Já existe tipo de caminhão com o nome " + tipo.nome());
            }
            TipoCaminhao salvo = caminhoes.salvar(tipo);
            auditoria.registrar(solicitante, tipo.id() == null ? "CRIAR" : "ALTERAR", "TIPO_CAMINHAO", salvo.id(),
                    Map.of("nome", salvo.nome(), "ativo", salvo.ativo()));
            return salvo;
        });
    }

    // ---- Tipos de paletização
    public List<TipoPaletizacao> listarTiposPaletizacao() {
        return paletizacoes.listar().stream().sorted(Comparator.comparing(TipoPaletizacao::nome)).toList();
    }

    public TipoPaletizacao salvarTipoPaletizacao(TipoPaletizacao tipo, Solicitante solicitante) {
        return transacao.executar(() -> {
            if (tipo.id() != null) {
                paletizacoes.buscarPorId(tipo.id()).orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de paletização", tipo.id()));
            }
            if (paletizacoes.existeNome(tipo.nome(), tipo.id())) {
                throw new RegraNegocioException("Já existe tipo de paletização com o nome " + tipo.nome());
            }
            TipoPaletizacao salvo = paletizacoes.salvar(tipo);
            auditoria.registrar(solicitante, tipo.id() == null ? "CRIAR" : "ALTERAR", "TIPO_PALETIZACAO", salvo.id(),
                    Map.of("nome", salvo.nome(), "fator", salvo.fatorMultiplicador(), "ativo", salvo.ativo()));
            return salvo;
        });
    }
}
