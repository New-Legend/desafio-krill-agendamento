package br.com.redekrill.agendamento.infrastructure.persistence.jpa;

import br.com.redekrill.agendamento.infrastructure.persistence.entity.ValePaleteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ValePaleteJpa extends JpaRepository<ValePaleteEntity, Long>, JpaSpecificationExecutor<ValePaleteEntity> {
    Optional<ValePaleteEntity> findByCodigo(String codigo);

    @Query(value = "select nextval('vale_palete_seq')", nativeQuery = true)
    long proximaSequencia();
}
