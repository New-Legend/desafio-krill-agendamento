package br.com.redekrill.agendamento.infrastructure.persistence.jpa;

import br.com.redekrill.agendamento.infrastructure.persistence.entity.PushInscricaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PushInscricaoJpa extends JpaRepository<PushInscricaoEntity, Long> {
    Optional<PushInscricaoEntity> findByEndpoint(String endpoint);

    @Transactional
    void deleteByEndpoint(String endpoint);
}
