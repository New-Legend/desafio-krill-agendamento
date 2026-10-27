package br.com.redekrill.agendamento.infrastructure.persistence.adapter;

import br.com.redekrill.agendamento.application.port.ParametroRepositorio;
import br.com.redekrill.agendamento.domain.model.ParametroCapacidade;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.ParametroCapacidadeEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.ParametroCapacidadeJpa;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class ParametroRepositorioJpa implements ParametroRepositorio {
    private static final short ID = 1;
    private final ParametroCapacidadeJpa jpa;

    public ParametroRepositorioJpa(ParametroCapacidadeJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public ParametroCapacidade obter() {
        return jpa.findById(ID)
                .map(e -> new ParametroCapacidade(e.capacidadeDiaria, e.quantidadeFaixas, e.duracaoFaixaMinutos))
                .orElse(ParametroCapacidade.PADRAO);
    }

    @Override
    public ParametroCapacidade salvar(ParametroCapacidade p) {
        ParametroCapacidadeEntity e = jpa.findById(ID).orElseGet(ParametroCapacidadeEntity::new);
        e.capacidadeDiaria = p.capacidadeDiariaPaletes();
        e.quantidadeFaixas = p.quantidadeFaixas();
        e.duracaoFaixaMinutos = p.duracaoFaixaMinutos();
        e.atualizadoEm = OffsetDateTime.now();
        jpa.save(e);
        return p;
    }
}
