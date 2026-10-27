package br.com.redekrill.agendamento.infrastructure.persistence.jpa;

import br.com.redekrill.agendamento.infrastructure.persistence.entity.AgendamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface AgendamentoJpa extends JpaRepository<AgendamentoEntity, Long>, JpaSpecificationExecutor<AgendamentoEntity> {
    List<AgendamentoEntity> findByDataBetween(LocalDate inicio, LocalDate fim);
}
