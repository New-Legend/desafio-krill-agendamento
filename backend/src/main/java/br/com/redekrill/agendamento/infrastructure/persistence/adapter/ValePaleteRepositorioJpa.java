package br.com.redekrill.agendamento.infrastructure.persistence.adapter;

import br.com.redekrill.agendamento.application.port.ValePaleteRepositorio;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import br.com.redekrill.agendamento.domain.model.ValePalete.Assinatura;
import br.com.redekrill.agendamento.domain.model.ValePalete.Quantidades;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.ValePaleteEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.ValePaleteJpa;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ValePaleteRepositorioJpa implements ValePaleteRepositorio {
    private final ValePaleteJpa jpa;

    public ValePaleteRepositorioJpa(ValePaleteJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public ValePalete salvar(ValePalete v) {
        ValePaleteEntity e = new ValePaleteEntity();
        e.id = v.getId();
        e.codigo = v.getCodigo();
        e.agendamentoId = v.getAgendamentoId();
        e.data = v.getData();
        e.fornecedor = v.getFornecedor();
        e.placa = v.getPlaca();
        e.motorista = v.getMotorista();
        e.pbr = v.getEntregues().pbr();
        e.chep = v.getEntregues().chep();
        e.chapatex = v.getEntregues().chapatex();
        e.descartaveis = v.getEntregues().descartaveis();
        e.devolvidosPbr = v.getDevolvidos().pbr();
        e.devolvidosChep = v.getDevolvidos().chep();
        e.devolvidosChapatex = v.getDevolvidos().chapatex();
        e.devolvidosDescartaveis = v.getDevolvidos().descartaveis();
        e.status = v.getStatus().name();
        e.observacao = v.getObservacao();
        if (v.getAssinaturaMotorista() != null) {
            e.assinaturaMotorista = v.getAssinaturaMotorista().imagem();
            e.assinaturaMotoristaNome = v.getAssinaturaMotorista().nome();
            e.assinaturaMotoristaEm = v.getAssinaturaMotorista().em();
        }
        if (v.getAssinaturaConferente() != null) {
            e.assinaturaConferente = v.getAssinaturaConferente().imagem();
            e.assinaturaConferenteNome = v.getAssinaturaConferente().nome();
            e.assinaturaConferenteEm = v.getAssinaturaConferente().em();
        }
        e.criadoPor = v.getCriadoPor();
        e.criadoEm = v.getCriadoEm();
        return dominio(jpa.save(e));
    }

    @Override
    public Optional<ValePalete> buscarPorId(Long id) {
        return jpa.findById(id).map(ValePaleteRepositorioJpa::dominio);
    }

    @Override
    public Optional<ValePalete> buscarPorCodigo(String codigo) {
        return jpa.findByCodigo(codigo).map(ValePaleteRepositorioJpa::dominio);
    }

    @Override
    public List<ValePalete> listar(LocalDate inicio, LocalDate fim, ValePalete.Status status, String fornecedor) {
        Specification<ValePaleteEntity> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (inicio != null) p.add(cb.greaterThanOrEqualTo(root.get("data"), inicio));
            if (fim != null) p.add(cb.lessThanOrEqualTo(root.get("data"), fim));
            if (status != null) p.add(cb.equal(root.get("status"), status.name()));
            if (fornecedor != null && !fornecedor.isBlank()) {
                p.add(cb.like(cb.lower(root.get("fornecedor")), "%" + fornecedor.trim().toLowerCase() + "%"));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return jpa.findAll(spec, Sort.by(Sort.Direction.DESC, "data", "id")).stream().map(ValePaleteRepositorioJpa::dominio).toList();
    }

    @Override
    public long proximaSequencia() {
        return jpa.proximaSequencia();
    }

    static ValePalete dominio(ValePaleteEntity e) {
        return new ValePalete(e.id, e.codigo, e.agendamentoId, e.data, e.fornecedor, e.placa, e.motorista,
                new Quantidades(e.pbr, e.chep, e.chapatex, e.descartaveis),
                new Quantidades(e.devolvidosPbr, e.devolvidosChep, e.devolvidosChapatex, e.devolvidosDescartaveis),
                ValePalete.Status.valueOf(e.status), e.observacao,
                e.assinaturaMotorista == null ? null : new Assinatura(e.assinaturaMotoristaNome, e.assinaturaMotorista, e.assinaturaMotoristaEm),
                e.assinaturaConferente == null ? null : new Assinatura(e.assinaturaConferenteNome, e.assinaturaConferente, e.assinaturaConferenteEm),
                e.criadoPor, e.criadoEm);
    }
}
