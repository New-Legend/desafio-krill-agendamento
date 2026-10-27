package br.com.redekrill.agendamento.infrastructure.persistence.adapter;

import br.com.redekrill.agendamento.application.port.TipoCaminhaoRepositorio;
import br.com.redekrill.agendamento.domain.model.TipoCaminhao;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.TipoCaminhaoEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.TipoCaminhaoJpa;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TipoCaminhaoRepositorioJpa implements TipoCaminhaoRepositorio {
    private final TipoCaminhaoJpa jpa;

    public TipoCaminhaoRepositorioJpa(TipoCaminhaoJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<TipoCaminhao> listar() {
        return jpa.findAll().stream().map(TipoCaminhaoRepositorioJpa::dominio).toList();
    }

    @Override
    public Optional<TipoCaminhao> buscarPorId(Long id) {
        return jpa.findById(id).map(TipoCaminhaoRepositorioJpa::dominio);
    }

    @Override
    public boolean existeNome(String nome, Long ignorarId) {
        return jpa.existeNome(nome, ignorarId);
    }

    @Override
    public TipoCaminhao salvar(TipoCaminhao t) {
        TipoCaminhaoEntity e = new TipoCaminhaoEntity();
        e.id = t.id();
        e.nome = t.nome();
        e.descricao = t.descricao();
        e.capacidadePaletes = t.capacidadePaletes();
        e.ativo = t.ativo();
        return dominio(jpa.save(e));
    }

    private static TipoCaminhao dominio(TipoCaminhaoEntity e) {
        return new TipoCaminhao(e.id, e.nome, e.descricao, e.capacidadePaletes, e.ativo);
    }
}
