package br.com.redekrill.agendamento.infrastructure.persistence.jpa;

import br.com.redekrill.agendamento.infrastructure.persistence.entity.ParametroCapacidadeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametroCapacidadeJpa extends JpaRepository<ParametroCapacidadeEntity, Short> {
}
