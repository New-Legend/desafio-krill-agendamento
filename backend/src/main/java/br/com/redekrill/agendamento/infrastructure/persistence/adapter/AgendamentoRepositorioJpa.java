package br.com.redekrill.agendamento.infrastructure.persistence.adapter;

import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.AgendamentoEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.AgendamentoJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AgendamentoRepositorioJpa implements AgendamentoRepositorio {

    private final AgendamentoJpa jpa;
    private final EntityManager em;

    public AgendamentoRepositorioJpa(AgendamentoJpa jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public Agendamento salvar(Agendamento a) {
        return paraDominio(jpa.save(paraEntidade(a)));
    }

    @Override
    public Optional<Agendamento> buscarPorId(Long id) {
        return jpa.findById(id).map(AgendamentoRepositorioJpa::paraDominio);
    }

    @Override
    public List<Agendamento> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return jpa.findByDataBetween(inicio, fim).stream().map(AgendamentoRepositorioJpa::paraDominio).toList();
    }

    @Override
    public List<Agendamento> listar(FiltroAgendamento f) {
        Specification<AgendamentoEntity> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.inicio() != null) p.add(cb.greaterThanOrEqualTo(root.get("data"), f.inicio()));
            if (f.fim() != null) p.add(cb.lessThanOrEqualTo(root.get("data"), f.fim()));
            if (f.status() != null) p.add(cb.equal(root.get("status"), f.status()));
            if (f.idPedido() != null) p.add(cb.equal(root.get("idPedido"), f.idPedido()));
            if (f.cdId() != null) p.add(cb.equal(root.get("cdId"), f.cdId()));
            if (f.divergentes()) p.add(cb.isNotNull(root.get("divergencia")));
            if (f.faixaHorariaId() != null) p.add(cb.equal(root.get("faixaHorariaId"), f.faixaHorariaId()));
            if (f.fornecedor() != null && !f.fornecedor().isBlank()) {
                p.add(cb.like(cb.lower(root.get("fornecedor")), "%" + f.fornecedor().trim().toLowerCase() + "%"));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return jpa.findAll(spec, Sort.by("data", "faixaHorariaId", "id")).stream()
                .map(AgendamentoRepositorioJpa::paraDominio).toList();
    }

    @Override
    public Optional<Agendamento> buscarPorTokenHash(String hash) {
        return jpa.findByTokenFornecedorHash(hash).map(AgendamentoRepositorioJpa::paraDominio);
    }

    @Override
    public void bloquearDia(LocalDate data) {
        em.createNativeQuery("select 1 from pg_advisory_xact_lock(?1)")
                .setParameter(1, 7_000_000L + data.toEpochDay())
                .getSingleResult();
    }

    static AgendamentoEntity paraEntidade(Agendamento a) {
        AgendamentoEntity e = new AgendamentoEntity();
        e.id = a.getId();
        e.data = a.getData();
        e.faixaHorariaId = a.getFaixaHorariaId();
        e.idPedido = a.getIdPedido();
        e.fornecedor = a.getFornecedor();
        e.emailFornecedor = a.getEmailFornecedor();
        e.tipoCaminhaoId = a.getTipoCaminhaoId();
        e.tipoPaletizacaoId = a.getTipoPaletizacaoId();
        e.quantidadePaletes = a.getQuantidadePaletes();
        e.paletesEquivalentes = a.getPaletesEquivalentes();
        e.observacao = a.getObservacao();
        e.status = a.getStatus();
        e.criadoPor = a.getCriadoPor();
        e.criadoEm = a.getCriadoEm();
        e.canceladoPor = a.getCanceladoPor();
        e.canceladoEm = a.getCanceladoEm();
        e.motivoCancelamento = a.getMotivoCancelamento();
        e.placa = a.getPlaca();
        e.motorista = a.getMotorista();
        e.chegadaEm = a.getChegadaEm();
        e.conferente = a.getConferente();
        e.inicioRecebimentoEm = a.getInicioRecebimentoEm();
        e.fimRecebimentoEm = a.getFimRecebimentoEm();
        e.paletesRecebidos = a.getPaletesRecebidos();
        e.divergencia = a.getDivergencia();
        e.tokenFornecedorHash = a.getTokenFornecedorHash();
        e.cdId = a.getCdId();
        e.fornecedorId = a.getFornecedorId();
        return e;
    }

    static Agendamento paraDominio(AgendamentoEntity e) {
        return Agendamento.builder().id(e.id).data(e.data).faixaHorariaId(e.faixaHorariaId).idPedido(e.idPedido)
                .fornecedor(e.fornecedor).emailFornecedor(e.emailFornecedor).tipoCaminhaoId(e.tipoCaminhaoId)
                .tipoPaletizacaoId(e.tipoPaletizacaoId).quantidadePaletes(e.quantidadePaletes)
                .paletesEquivalentes(e.paletesEquivalentes).observacao(e.observacao).status(e.status)
                .criadoPor(e.criadoPor).criadoEm(e.criadoEm).canceladoPor(e.canceladoPor)
                .canceladoEm(e.canceladoEm).motivoCancelamento(e.motivoCancelamento).placa(e.placa).motorista(e.motorista)
                .chegadaEm(e.chegadaEm).conferente(e.conferente).inicioRecebimentoEm(e.inicioRecebimentoEm)
                .fimRecebimentoEm(e.fimRecebimentoEm).paletesRecebidos(e.paletesRecebidos).divergencia(e.divergencia)
                .tokenFornecedorHash(e.tokenFornecedorHash).cdId(e.cdId).fornecedorId(e.fornecedorId).build();
    }
}
