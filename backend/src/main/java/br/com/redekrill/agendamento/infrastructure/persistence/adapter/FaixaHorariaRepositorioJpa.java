package br.com.redekrill.agendamento.infrastructure.persistence.adapter;

import br.com.redekrill.agendamento.application.port.FaixaHorariaRepositorio;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.FaixaHorariaEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.FaixaHorariaJpa;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FaixaHorariaRepositorioJpa implements FaixaHorariaRepositorio {
    private final FaixaHorariaJpa jpa;

    public FaixaHorariaRepositorioJpa(FaixaHorariaJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<FaixaHoraria> listar() {
        return jpa.findAll().stream().map(FaixaHorariaRepositorioJpa::dominio).toList();
    }

    @Override
    public Optional<FaixaHoraria> buscarPorId(Long id) {
        return jpa.findById(id).map(FaixaHorariaRepositorioJpa::dominio);
    }

    @Override
    public FaixaHoraria salvar(FaixaHoraria f) {
        FaixaHorariaEntity e = new FaixaHorariaEntity();
        e.id = f.id();
        e.horaInicio = f.horaInicio();
        e.horaFim = f.horaFim();
        e.capacidadePaletes = f.capacidadePaletes();
        e.ativo = f.ativo();
        return dominio(jpa.save(e));
    }

    private static FaixaHoraria dominio(FaixaHorariaEntity e) {
        return new FaixaHoraria(e.id, e.horaInicio, e.horaFim, e.capacidadePaletes, e.ativo);
    }
}
