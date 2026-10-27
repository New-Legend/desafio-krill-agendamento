package br.com.redekrill.agendamento.infrastructure.persistence.adapter;

import br.com.redekrill.agendamento.application.dto.FiltroAuditoria;
import br.com.redekrill.agendamento.application.dto.Pagina;
import br.com.redekrill.agendamento.application.dto.RegistroAuditoria;
import br.com.redekrill.agendamento.application.port.AuditoriaRepositorio;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.AuditoriaEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.AuditoriaJpa;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuditoriaRepositorioJpa implements AuditoriaRepositorio {
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private final AuditoriaJpa jpa;

    public AuditoriaRepositorioJpa(AuditoriaJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(RegistroAuditoria r) {
        AuditoriaEntity e = new AuditoriaEntity();
        e.dataHora = r.dataHora();
        e.usuario = r.usuario();
        e.acao = r.acao();
        e.entidade = r.entidade();
        e.entidadeId = r.entidadeId();
        e.ip = r.ip();
        e.sucesso = r.sucesso();
        e.detalhes = r.detalhes();
        jpa.save(e);
    }

    @Override
    public Pagina<RegistroAuditoria> listar(FiltroAuditoria f, int pagina, int tamanho) {
        Specification<AuditoriaEntity> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.inicio() != null) p.add(cb.greaterThanOrEqualTo(root.get("dataHora"), f.inicio().atStartOfDay(ZONA).toOffsetDateTime()));
            if (f.fim() != null) p.add(cb.lessThan(root.get("dataHora"), f.fim().plusDays(1).atStartOfDay(ZONA).toOffsetDateTime()));
            if (f.usuario() != null && !f.usuario().isBlank()) p.add(cb.equal(cb.lower(root.get("usuario")), f.usuario().trim().toLowerCase()));
            if (f.acao() != null && !f.acao().isBlank()) p.add(cb.equal(root.get("acao"), f.acao().trim().toUpperCase()));
            if (f.entidade() != null && !f.entidade().isBlank()) p.add(cb.equal(root.get("entidade"), f.entidade().trim().toUpperCase()));
            return cb.and(p.toArray(Predicate[]::new));
        };
        Page<AuditoriaEntity> page = jpa.findAll(spec, PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "dataHora", "id")));
        return new Pagina<>(page.getContent().stream()
                .map(e -> new RegistroAuditoria(e.id, e.dataHora, e.usuario, e.acao, e.entidade, e.entidadeId, e.ip,
                        e.sucesso, e.detalhes)).toList(),
                page.getTotalElements(), pagina, tamanho);
    }
}
