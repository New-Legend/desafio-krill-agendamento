package br.com.redekrill.agendamento.infrastructure.persistence.jpa;

import br.com.redekrill.agendamento.infrastructure.persistence.entity.FaixaHorariaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaixaHorariaJpa extends JpaRepository<FaixaHorariaEntity, Long> {
}
