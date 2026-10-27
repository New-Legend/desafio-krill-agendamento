package br.com.redekrill.agendamento.infrastructure.persistence.adapter;

import br.com.redekrill.agendamento.application.port.TipoPaletizacaoRepositorio;
import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.TipoPaletizacaoEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.TipoPaletizacaoJpa;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TipoPaletizacaoRepositorioJpa implements TipoPaletizacaoRepositorio {
    private final TipoPaletizacaoJpa jpa;

    public TipoPaletizacaoRepositorioJpa(TipoPaletizacaoJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<TipoPaletizacao> listar() {
        return jpa.findAll().stream().map(TipoPaletizacaoRepositorioJpa::dominio).toList();
    }

    @Override
    public Optional<TipoPaletizacao> buscarPorId(Long id) {
        return jpa.findById(id).map(TipoPaletizacaoRepositorioJpa::dominio);
    }

    @Override
    public boolean existeNome(String nome, Long ignorarId) {
        return jpa.existeNome(nome, ignorarId);
    }

    @Override
    public TipoPaletizacao salvar(TipoPaletizacao t) {
        TipoPaletizacaoEntity e = new TipoPaletizacaoEntity();
        e.id = t.id();
        e.nome = t.nome();
        e.fatorMultiplicador = t.fatorMultiplicador();
        e.ativo = t.ativo();
        return dominio(jpa.save(e));
    }

    private static TipoPaletizacao dominio(TipoPaletizacaoEntity e) {
        return new TipoPaletizacao(e.id, e.nome, e.fatorMultiplicador, e.ativo);
    }
}
