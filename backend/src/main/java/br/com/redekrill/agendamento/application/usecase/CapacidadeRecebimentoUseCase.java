package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.CapacidadeRecebimentoRepositorio;
import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.application.port.LogisticaRepositorio;
import br.com.redekrill.agendamento.application.port.ParametroRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.CapacidadeRecebimento;
import br.com.redekrill.agendamento.domain.model.CentroDistribuicao;
import br.com.redekrill.agendamento.domain.model.OcupacaoRecebimento;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Capacidade diária de recebimento por CD e por loja (entrega direta). Sem regra cadastrada, o CD usa a
 * capacidade diária própria (ou a geral de 480 paletes) e a loja fica sem limite.
 */
public class CapacidadeRecebimentoUseCase {

    public record Dados(Long cdId, String loja, Integer diaSemana, Integer paletes, Integer caminhoes) {
    }

    private final CapacidadeRecebimentoRepositorio repo;
    private final LogisticaRepositorio logistica;
    private final CentroDistribuicaoRepositorio cds;
    private final ParametroRepositorio parametros;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;

    public CapacidadeRecebimentoUseCase(CapacidadeRecebimentoRepositorio repo, LogisticaRepositorio logistica,
                                        CentroDistribuicaoRepositorio cds, ParametroRepositorio parametros,
                                        AuditoriaUseCase auditoria, Transacao transacao) {
        this.repo = repo;
        this.logistica = logistica;
        this.cds = cds;
        this.parametros = parametros;
        this.auditoria = auditoria;
        this.transacao = transacao;
    }

    public List<CapacidadeRecebimento> listar() {
        return repo.listar();
    }

    public CapacidadeRecebimento salvar(Long id, Dados d, Solicitante s) {
        return transacao.executar(() -> {
            if (id != null) repo.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Capacidade", id));
            String loja = d.loja() == null || d.loja().isBlank() ? null : d.loja().trim();
            Long cd = loja == null ? d.cdId() : null;
            if (cd != null) cds.buscarPorId(cd).orElseThrow(() -> new RecursoNaoEncontradoException("CD", cd));
            CapacidadeRecebimento c = new CapacidadeRecebimento(id, cd, loja, d.diaSemana(), d.paletes(), d.caminhoes());
            boolean duplicada = repo.listar().stream().anyMatch(o -> !Objects.equals(o.id(), id)
                    && Objects.equals(o.cdId(), c.cdId()) && Objects.equals(o.diaSemana(), c.diaSemana())
                    && (o.loja() == null ? c.loja() == null : o.loja().equalsIgnoreCase(c.loja() == null ? "" : c.loja())));
            if (duplicada) throw new RegraNegocioException("Já existe capacidade cadastrada para este destino e dia");
            CapacidadeRecebimento salvo = repo.salvar(c);
            Map<String, Object> det = new HashMap<>();
            det.put("destino", c.loja() == null ? "CD " + c.cdId() : c.loja());
            det.put("diaSemana", c.diaSemana() == null ? "todos" : c.diaSemana());
            det.put("paletes", c.paletes() == null ? "-" : c.paletes());
            det.put("caminhoes", c.caminhoes() == null ? "-" : c.caminhoes());
            auditoria.registrar(s, id == null ? "CRIAR" : "ALTERAR", "CAPACIDADE_RECEBIMENTO", salvo.id(), det);
            return salvo;
        });
    }

    public void excluir(Long id, Solicitante s) {
        transacao.executar(() -> {
            repo.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Capacidade", id));
            repo.excluir(id);
            auditoria.registrar(s, "EXCLUIR", "CAPACIDADE_RECEBIMENTO", id, Map.of());
            return null;
        });
    }

    /** Ocupação do dia: loja informada = entrega direta na loja; senão, o CD (nulo = CD padrão). */
    public OcupacaoRecebimento ocupacao(LocalDate data, Long cdId, String loja) {
        if (data == null) throw new RegraNegocioException("Data é obrigatória");
        String l = loja == null || loja.isBlank() ? null : loja.trim();
        Long cd = l != null ? null : (cdId == null ? 1L : cdId);
        List<CapacidadeRecebimento> regras = repo.listar().stream()
                .filter(r -> l == null ? cd.equals(r.cdId()) : r.loja() != null && r.loja().equalsIgnoreCase(l)).toList();
        CapacidadeRecebimento regra = CapacidadeRecebimento.resolver(regras, data).orElse(null);
        Integer capPaletes = regra == null ? null : regra.paletes();
        Integer capCaminhoes = regra == null ? null : regra.caminhoes();
        String destino = l == null ? "o CD " + cd : "a loja " + l;
        if (l == null) {
            CentroDistribuicao c = cds.buscarPorId(cd).orElseThrow(() -> new RecursoNaoEncontradoException("CD", cd));
            destino = "o CD " + c.codigo();
            if (capPaletes == null) capPaletes = c.capacidadeDia(parametros.obter().capacidadeDiariaPaletes());
        }
        LogisticaRepositorio.Consumo consumo = logistica.consumo(data, cd, l);
        return new OcupacaoRecebimento(data, destino, capPaletes, consumo.paletes(), capCaminhoes, consumo.caminhoes());
    }
}
